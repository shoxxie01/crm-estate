package pl.delta.crm.property;

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
import pl.delta.crm.property.dictionary.Portal;
import pl.delta.crm.property.dictionary.PublicationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Stan publikacji oferty w jednym portalu.
 *
 * <p>Jedna oferta żyje równolegle w kilku serwisach i w każdym ma inny
 * identyfikator zewnętrzny, inny moment ostatniej wysyłki i własny błąd.
 * Trzymanie tego w kolumnach tabeli {@code properties} („otodom_id",
 * „morizon_id"…) zmuszałoby do migracji schematu przy każdym nowym portalu.
 */
@Entity
@Table(name = "property_portal_publications")
public class PortalPublication {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Enumerated(EnumType.STRING)
    @Column(name = "portal", nullable = false, length = 30)
    private Portal portal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PublicationStatus status = PublicationStatus.NIEOPUBLIKOWANA;

    @Column(name = "external_id", length = 64)
    private String externalId;

    @Column(name = "external_url", length = 500)
    private String externalUrl;

    @Column(name = "last_exported_at")
    private Instant lastExportedAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    protected PortalPublication() {
        // wymagane przez JPA
    }

    public PortalPublication(Property property, Portal portal) {
        this.property = property;
        this.portal = portal;
    }

    public void markSent() {
        this.status = PublicationStatus.WYSLANA;
        this.lastExportedAt = Instant.now();
        this.lastError = null;
    }

    public void markPublished(String externalId, String externalUrl) {
        this.status = PublicationStatus.OPUBLIKOWANA;
        this.externalId = externalId;
        this.externalUrl = externalUrl;
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status = PublicationStatus.BLAD;
        this.lastError = error;
    }

    public UUID getId() {
        return id;
    }

    public Property getProperty() {
        return property;
    }

    public Portal getPortal() {
        return portal;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public void setStatus(PublicationStatus status) {
        this.status = status;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public Instant getLastExportedAt() {
        return lastExportedAt;
    }

    public String getLastError() {
        return lastError;
    }
}
