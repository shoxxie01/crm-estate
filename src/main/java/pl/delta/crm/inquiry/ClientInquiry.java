package pl.delta.crm.inquiry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.delta.crm.agency.Agency;
import pl.delta.crm.client.Client;
import pl.delta.crm.user.User;

import java.time.Instant;
import java.util.UUID;

/**
 * Zgłoszenie z publicznego formularza. Poczekalnia przed bazą klientów.
 *
 * <p>Kupujący zostawia kryteria, sprzedający. Opis nieruchomości. Oba jako
 * JSON w kształcie formularza, a nie w osobnych kolumnach: po zgłoszeniu nikt
 * nie filtruje, a przy przyjęciu i tak powstają z nich właściwe dane klienta.
 */
@Entity
@Table(name = "client_inquiries")
public class ClientInquiry {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false, updatable = false)
    private Agency agency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InquiryStatus status = InquiryStatus.NEW;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 190)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "intent", nullable = false, length = 10, updatable = false)
    private InquiryIntent intent;

    /** JSON kryteriów ({@code RequirementRequest}). Tylko przy kupnie. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "criteria")
    private String criteria;

    /** JSON opisu nieruchomości ({@code SaleOfferRequest}). Tylko przy sprzedaży. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "offer")
    private String offer;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "message")
    private String message;

    @Column(name = "consent_processing_at", nullable = false, updatable = false)
    private Instant consentProcessingAt;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "consent_text", nullable = false, updatable = false)
    private String consentText;

    @Column(name = "consent_marketing", nullable = false, updatable = false)
    private boolean consentMarketing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handledBy;

    @Column(name = "handled_at")
    private Instant handledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ClientInquiry() {
        // wymagane przez JPA
    }

    /**
     * @param payload JSON kryteriów przy kupnie albo opisu nieruchomości przy sprzedaży
     */
    public ClientInquiry(Agency agency, InquiryIntent intent, String firstName, String lastName, String phone,
                         String email, String payload, String message, String consentText, boolean consentMarketing) {
        this.agency = agency;
        this.intent = intent;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        if (intent == InquiryIntent.SELL) {
            this.offer = payload;
        } else {
            this.criteria = payload;
        }
        this.message = message;
        this.consentText = consentText;
        this.consentMarketing = consentMarketing;
        this.consentProcessingAt = this.createdAt;
    }

    /** Zgłoszenie przyjęte. Do nowego albo istniejącego klienta. */
    public void convert(Client client, User handledBy) {
        this.status = InquiryStatus.CONVERTED;
        this.client = client;
        this.handledBy = handledBy;
        this.handledAt = Instant.now();
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

    public InquiryStatus getStatus() {
        return status;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public InquiryIntent getIntent() {
        return intent;
    }

    public String getCriteria() {
        return criteria;
    }

    public String getOffer() {
        return offer;
    }

    public String getMessage() {
        return message;
    }

    public Instant getConsentProcessingAt() {
        return consentProcessingAt;
    }

    public String getConsentText() {
        return consentText;
    }

    public boolean isConsentMarketing() {
        return consentMarketing;
    }

    public Client getClient() {
        return client;
    }

    public User getHandledBy() {
        return handledBy;
    }

    public Instant getHandledAt() {
        return handledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
