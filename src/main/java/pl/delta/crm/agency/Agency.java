package pl.delta.crm.agency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Biuro nieruchomości. Wszystkie dane domenowe (oferty, klienci, umowy) należą
 * do agencji, nie do pojedynczego użytkownika — to po niej filtrujemy dostęp.
 */
@Entity
@Table(name = "agencies")
public class Agency {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    /** E-mail konta biura w portalu — Otodom rozpoznaje po nim nadawcę paczki. */
    @Column(length = 190)
    private String contactEmail;

    @Column(length = 30)
    private String contactPhone;

    @Column(length = 50)
    private String licenseNumber;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Agency() {
        // wymagane przez JPA
    }

    public Agency(String name, String contactEmail) {
        this.name = name;
        this.contactEmail = contactEmail;
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
