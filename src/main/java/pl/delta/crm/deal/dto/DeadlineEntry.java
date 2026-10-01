package pl.delta.crm.deal.dto;

import pl.delta.crm.deal.DealDeadline;
import pl.delta.crm.deal.dictionary.DeadlineStatus;
import pl.delta.crm.deal.dictionary.DeadlineType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Termin umowny na karcie transakcji. Razem z datą, którą zastąpił (aneks). */
public record DeadlineEntry(
        UUID id,
        DeadlineType type,
        LocalDate dueDate,
        DeadlineStatus status,
        String note,
        /** Poprzednia data, gdy termin powstał przez przesunięcie. */
        LocalDate movedFromDate,
        Instant resolvedAt
) {

    public static DeadlineEntry from(DealDeadline deadline) {
        DealDeadline previous = deadline.getMovedFrom();
        return new DeadlineEntry(
                deadline.getId(),
                deadline.getType(),
                deadline.getDueDate(),
                deadline.getStatus(),
                deadline.getNote(),
                previous == null ? null : previous.getDueDate(),
                deadline.getResolvedAt()
        );
    }
}
