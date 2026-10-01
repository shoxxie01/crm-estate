package pl.delta.crm.deal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.delta.crm.agency.Agency;
import pl.delta.crm.client.Client;
import pl.delta.crm.deal.dictionary.DealLostReason;
import pl.delta.crm.deal.dictionary.DealStage;
import pl.delta.crm.property.Property;
import pl.delta.crm.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Transakcja. Karta tablicy Kanban.
 *
 * <p>Jedno zlecenie po stronie podaży: właściciel ({@link #client}) powierza
 * biuru ofertę ({@link #property}), a od etapu negocjacji pojawia się druga
 * strona ({@link #buyer}). Wszystkie trzy powiązania są opcjonalne, bo karta
 * powstaje już przy pierwszym telefonie. Zanim jest oferta i karta klienta.
 *
 * <p>Etap zmienia się wyłącznie przez {@link #moveTo}, żeby data wejścia do
 * etapu, data zamknięcia i powód przegranej nie rozjechały się ze sobą.
 * Te same reguły pilnują CHECK-i w migracji {@code V16}.
 */
@Entity
@Table(name = "deals")
public class Deal {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    private DealStage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private Client buyer;

    @Column(name = "deal_value", precision = 14, scale = 2)
    private BigDecimal value;

    @Column(name = "commission", precision = 14, scale = 2)
    private BigDecimal commission;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "notes")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "lost_reason", length = 30)
    private DealLostReason lostReason;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "lost_note")
    private String lostNote;

    /** Od kiedy karta stoi w obecnej kolumnie. Z tego tablica liczy „dni w etapie". */
    @Column(name = "stage_changed_at", nullable = false)
    private Instant stageChangedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Deal() {
        // wymagane przez JPA
    }

    /**
     * Nowa karta zawsze startuje w otwartym etapie. Zamknięcie idzie przez
     * {@link #moveTo}, bo przegrana wymaga powodu.
     */
    public Deal(Agency agency, User agent, User createdBy, String title, DealStage stage) {
        if (stage.isClosed()) {
            throw new IllegalArgumentException("Nowa transakcja nie może być od razu zamknięta.");
        }
        this.agency = agency;
        this.agent = agent;
        this.createdBy = createdBy;
        this.title = title;
        this.stage = stage;
        this.stageChangedAt = createdAt;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    /**
     * Przeniesienie karty do innej kolumny. Powód przegranej ma sens tylko
     * w „Przegranych". Przy każdym innym etapie jest czyszczony, żeby karta
     * wyciągnięta z powrotem do lejka nie niosła starej przyczyny.
     *
     * @return {@code true}, gdy etap faktycznie się zmienił (trzeba dopisać historię)
     */
    public boolean moveTo(DealStage target, DealLostReason reason, String note) {
        boolean changed = target != stage;

        if (changed) {
            Instant now = Instant.now();
            this.stage = target;
            this.stageChangedAt = now;
            this.closedAt = target.isClosed() ? now : null;
        }

        boolean lost = target == DealStage.LOST;
        this.lostReason = lost ? reason : null;
        this.lostNote = lost ? note : null;
        return changed;
    }

    public UUID getId() {
        return id;
    }

    public Agency getAgency() {
        return agency;
    }

    public User getAgent() {
        return agent;
    }

    public void setAgent(User agent) {
        this.agent = agent;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DealStage getStage() {
        return stage;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }

    public Client getBuyer() {
        return buyer;
    }

    public void setBuyer(Client buyer) {
        this.buyer = buyer;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getCommission() {
        return commission;
    }

    public void setCommission(BigDecimal commission) {
        this.commission = commission;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public DealLostReason getLostReason() {
        return lostReason;
    }

    public String getLostNote() {
        return lostNote;
    }

    public Instant getStageChangedAt() {
        return stageChangedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
