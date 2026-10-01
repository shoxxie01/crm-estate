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
import jakarta.persistence.Table;
import pl.delta.crm.deal.dictionary.DealStage;
import pl.delta.crm.user.User;

import java.time.Instant;
import java.util.UUID;

/**
 * Jedno przejście karty między kolumnami. Wpisy są tylko dopisywane. Historia
 * jest podstawą raportów lejka (konwersja, czas w etapie), więc nie ma tu
 * żadnego settera.
 */
@Entity
@Table(name = "deal_stage_changes")
public class DealStageChange {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deal_id", nullable = false, updatable = false)
    private Deal deal;

    /** Pusty przy założeniu karty. */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_stage", length = 20, updatable = false)
    private DealStage fromStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_stage", nullable = false, length = 20, updatable = false)
    private DealStage toStage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false, updatable = false)
    private User changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt = Instant.now();

    protected DealStageChange() {
        // wymagane przez JPA
    }

    public DealStageChange(Deal deal, DealStage fromStage, DealStage toStage, User changedBy) {
        this.deal = deal;
        this.fromStage = fromStage;
        this.toStage = toStage;
        this.changedBy = changedBy;
    }

    public UUID getId() {
        return id;
    }

    public Deal getDeal() {
        return deal;
    }

    public DealStage getFromStage() {
        return fromStage;
    }

    public DealStage getToStage() {
        return toStage;
    }

    public User getChangedBy() {
        return changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
