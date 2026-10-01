package pl.delta.crm.deal.dto;

import pl.delta.crm.deal.Deal;
import pl.delta.crm.deal.DealDeadline;
import pl.delta.crm.deal.dictionary.DeadlineStatus;
import pl.delta.crm.deal.dictionary.DeadlineType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Termin umowny w kalendarzu. Wpis całodniowy tylko do odczytu. Kliknięcie
 * prowadzi do karty transakcji, bo to tam termin się zmienia.
 */
public record CalendarDeadline(
        UUID id,
        DeadlineType type,
        LocalDate dueDate,
        DeadlineStatus status,
        String note,
        UUID dealId,
        String dealTitle,
        UUID agentId,
        String agentName
) {

    public static CalendarDeadline from(DealDeadline deadline) {
        Deal deal = deadline.getDeal();
        return new CalendarDeadline(
                deadline.getId(),
                deadline.getType(),
                deadline.getDueDate(),
                deadline.getStatus(),
                deadline.getNote(),
                deal.getId(),
                deal.getTitle(),
                deal.getAgent().getId(),
                deal.getAgent().getFirstName() + " " + deal.getAgent().getLastName()
        );
    }
}
