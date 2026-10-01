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
import pl.delta.crm.client.Client;
import pl.delta.crm.deal.dictionary.InterestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Jedna osoba zainteresowana ofertą w ramach transakcji. Klient z bazy albo
 * samo imię i telefon (patrz komentarz w migracji {@code V17}).
 */
@Entity
@Table(name = "deal_interests")
public class DealInterest {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deal_id", nullable = false, updatable = false)
    private Deal deal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "name", length = 160)
    private String name;

    @Column(name = "phone", length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InterestStatus status = InterestStatus.NEW;

    @Column(name = "offer_amount", precision = 14, scale = 2)
    private BigDecimal offerAmount;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "note")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected DealInterest() {
        // wymagane przez JPA
    }

    public DealInterest(Deal deal) {
        this.deal = deal;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    /** Nazwa do wyświetlenia. Z karty klienta, a bez niej wpisana ręcznie. */
    public String displayName() {
        return client != null ? client.fullName() : name;
    }

    /** Telefon do wyświetlenia. Jak {@link #displayName()}. */
    public String displayPhone() {
        return client != null && client.getPhone() != null ? client.getPhone() : phone;
    }

    public UUID getId() {
        return id;
    }

    public Deal getDeal() {
        return deal;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public InterestStatus getStatus() {
        return status;
    }

    public void setStatus(InterestStatus status) {
        this.status = status;
    }

    public BigDecimal getOfferAmount() {
        return offerAmount;
    }

    public void setOfferAmount(BigDecimal offerAmount) {
        this.offerAmount = offerAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
