package pl.delta.crm.agency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Biuro nieruchomości. Wszystkie dane domenowe (oferty, klienci, umowy) należą
 * do agencji, nie do pojedynczego użytkownika. To po niej filtrujemy dostęp.
 */
@Entity
@Table(name = "agencies")
public class Agency {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    /** E-mail konta biura w portalu. Otodom rozpoznaje po nim nadawcę paczki. */
    @Column(length = 190)
    private String contactEmail;

    @Column(length = 30)
    private String contactPhone;

    @Column(length = 50)
    private String licenseNumber;

    /**
     * Klucz w adresie publicznego formularza zgłoszeniowego. Losowy, żeby nie
     * dało się zgadywać formularzy innych biur, i wymienialny. Patrz
     * {@link #regenerateIntakeToken()}.
     */
    @Column(name = "intake_token", nullable = false, unique = true, length = 40)
    private String intakeToken = newIntakeToken();

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Agency() {
        // wymagane przez JPA
    }

    public Agency(String name, String contactEmail) {
        this.name = name;
        this.contactEmail = contactEmail;
    }

    /** Stary link przestaje działać od razu. Na wypadek, gdy wyciekł do spamerów. */
    public void regenerateIntakeToken() {
        this.intakeToken = newIntakeToken();
    }

    private static String newIntakeToken() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    public String getIntakeToken() {
        return intakeToken;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
