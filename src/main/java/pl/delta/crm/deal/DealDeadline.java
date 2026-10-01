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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.delta.crm.deal.dictionary.DeadlineStatus;
import pl.delta.crm.deal.dictionary.DeadlineType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Termin umowny transakcji (patrz migracja {@code V18}). Status zmienia się
 * tylko przez {@link #markMet()} i {@link #markMoved()}, żeby data rozstrzygnięcia
 * nie rozjechała się ze statusem.
 */
@Entity
@Table(name = "deal_deadlines")
public class DealDeadline {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deal_id", nullable = false, updatable = false)
    private Deal deal;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private DeadlineType type;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeadlineStatus status = DeadlineStatus.OPEN;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "note")
    private String note;

    /** Termin, który ten zastąpił. Przy przesunięciu (aneksie). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moved_from_id", updatable = false)
    private DealDeadline movedFrom;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected DealDeadline() {
        // wymagane przez JPA
    }

    public DealDeadline(Deal deal, DeadlineType type, LocalDate dueDate, DealDeadline movedFrom) {
        this.deal = deal;
        this.type = type;
        this.dueDate = dueDate;
        this.movedFrom = movedFrom;
    }

    public void markMet() {
        this.status = DeadlineStatus.MET;
        this.resolvedAt = Instant.now();
    }

    public void markMoved() {
        this.status = DeadlineStatus.MOVED;
        this.resolvedAt = Instant.now();
    }

    /** Cofnięcie „dotrzymany". Np. kliknięte omyłkowo. */
    public void reopen() {
        this.status = DeadlineStatus.OPEN;
        this.resolvedAt = null;
    }

    public UUID getId() {
        return id;
    }

    public Deal getDeal() {
        return deal;
    }

    public DeadlineType getType() {
        return type;
    }

    public void setType(DeadlineType type) {
        this.type = type;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public DeadlineStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public DealDeadline getMovedFrom() {
        return movedFrom;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
