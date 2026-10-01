package pl.delta.crm.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.delta.crm.agency.Agency;
import pl.delta.crm.calendar.dictionary.EventOutcome;
import pl.delta.crm.calendar.dictionary.EventStatus;
import pl.delta.crm.calendar.dictionary.EventType;
import pl.delta.crm.client.Client;
import pl.delta.crm.deal.Deal;
import pl.delta.crm.deal.DealInterest;
import pl.delta.crm.property.Property;
import pl.delta.crm.user.User;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Wpis kalendarza biura.
 *
 * <p>Zdarzenie jest w tym CRM-ie łącznikiem, a nie bytem samodzielnym: wiąże
 * agenta z ofertą ({@link #property}) i z właścicielem ({@link #client}).
 * Oba wiązania są opcjonalne, bo połowa terminów powstaje, zanim będzie co
 * wiązać. Wycena poprzedza ofertę, a spotkanie akwizycyjne poprzedza klienta.
 *
 * <p>Kupujący i najemcy nie mają tu klucza obcego, tylko pola
 * {@link #counterpartyName} / {@link #counterpartyPhone}. W tej wersji CRM-u
 * {@link Client} to wyłącznie strona podaży (patrz komentarz w tamtej encji),
 * a dopisanie kupującego do tabeli klientów podważyłoby regułę, że rola klienta
 * wynika z typu transakcji jego ofert. Szczegóły w migracji {@code V10}.
 */
@Entity
@Table(name = "calendar_events")
public class CalendarEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    /** Czyj to termin. Kalendarz filtruje się po tym polu, nie po autorze wpisu. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    /** Oferta, której dotyczy termin. Z niej bierze się adres i numer oferty. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    /** Właściciel powiązany z terminem. Np. przy podpisaniu umowy pośrednictwa. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    /** Transakcja z tablicy Kanban, której krokiem jest ten termin. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id")
    private Deal deal;

    /**
     * Zainteresowani z transakcji, którzy biorą udział w terminie. Wspólne
     * oglądanie dwóch osób to jeden termin, ale każda z nich ma potem własny
     * status na liście zainteresowanych. Tylko przy powiązanej transakcji.
     */
    @ManyToMany
    @JoinTable(name = "calendar_event_participants",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "interest_id"))
    private Set<DealInterest> participants = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status = EventStatus.PLANNED;

    /** Wypełniany dopiero po fakcie. Wyłącznie przy statusie COMPLETED. */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 30)
    private EventOutcome outcome;

    /** Notatka z przebiegu. Osobno od opisu, żeby relacja nie nadpisała ustaleń sprzed terminu. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "outcome_note")
    private String outcomeNote;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "description")
    private String description;

    /** Miejsce wpisane ręcznie. Przy powiązanej ofercie adres bierze się z niej. */
    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    /**
     * Zdarzenie całodniowe (dzień otwarty, urlop). Zakres i tak jest zapisany
     * co do sekundy. Flaga mówi tylko tyle, że godziny nie należy pokazywać.
     */
    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    @Column(name = "counterparty_name", length = 160)
    private String counterpartyName;

    @Column(name = "counterparty_phone", length = 30)
    private String counterpartyPhone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected CalendarEvent() {
        // wymagane przez JPA
    }

    public CalendarEvent(Agency agency, User agent, User createdBy,
                         EventType type, String title, Instant startsAt, Instant endsAt) {
        this.agency = agency;
        this.agent = agent;
        this.createdBy = createdBy;
        this.type = type;
        this.title = title;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    /** Czy zakres tego zdarzenia zachodzi na podany. Stykające się krańce nie kolidują. */
    public boolean overlaps(Instant from, Instant to) {
        return startsAt.isBefore(to) && endsAt.isAfter(from);
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

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Deal getDeal() {
        return deal;
    }

    public void setDeal(Deal deal) {
        this.deal = deal;
    }

    public Set<DealInterest> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<DealInterest> participants) {
        this.participants.clear();
        this.participants.addAll(participants);
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public EventOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(EventOutcome outcome) {
        this.outcome = outcome;
    }

    public String getOutcomeNote() {
        return outcomeNote;
    }

    public void setOutcomeNote(String outcomeNote) {
        this.outcomeNote = outcomeNote;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }

    public String getCounterpartyName() {
        return counterpartyName;
    }

    public void setCounterpartyName(String counterpartyName) {
        this.counterpartyName = counterpartyName;
    }

    public String getCounterpartyPhone() {
        return counterpartyPhone;
    }

    public void setCounterpartyPhone(String counterpartyPhone) {
        this.counterpartyPhone = counterpartyPhone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
