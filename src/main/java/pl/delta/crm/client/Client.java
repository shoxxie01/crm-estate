package pl.delta.crm.client;

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
import pl.delta.crm.client.dictionary.ClientStatus;
import pl.delta.crm.client.dictionary.LeadSource;
import pl.delta.crm.user.User;

import java.time.Instant;
import java.util.UUID;

/**
 * Klient biura — właściciel zlecający obsługę swojej nieruchomości.
 *
 * <p>W tej wersji CRM-u klient jest zawsze osobą prywatną po stronie podaży
 * (nie ma poszukujących). Rozróżnienie „sprzedający" / „wynajmujący" celowo
 * <b>nie jest polem</b> tej encji — wynika z {@code TransactionType} ofert,
 * które klient powierzył biuru (patrz {@code Property.owner}). Jeden właściciel
 * może jedną nieruchomość sprzedawać, a inną wynajmować, więc trzymanie tej
 * cechy na osobie byłoby zwyczajnie nieprawdziwe.
 */
@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    /** Opiekun kontaktu. Z niego biorą się dane agenta prowadzącego relację. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    /**
     * Telefon jest w praktyce podstawowym kanałem kontaktu na rynku
     * nieruchomości, ale nie wymuszamy go twardo — część kontaktów przychodzi
     * mailem. Reguła „telefon albo e-mail" pilnowana jest w serwisie i przez
     * {@code CHECK} w migracji.
     */
    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 190)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20)
    private LeadSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClientStatus status = ClientStatus.ACTIVE;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Client() {
        // wymagane przez JPA
    }

    public Client(Agency agency, User agent, User createdBy, String firstName, String lastName) {
        this.agency = agency;
        this.agent = agent;
        this.createdBy = createdBy;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public String fullName() {
        return firstName + " " + lastName;
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LeadSource getSource() {
        return source;
    }

    public void setSource(LeadSource source) {
        this.source = source;
    }

    public ClientStatus getStatus() {
        return status;
    }

    public void setStatus(ClientStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
