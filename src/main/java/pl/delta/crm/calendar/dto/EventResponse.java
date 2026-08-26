package pl.delta.crm.calendar.dto;

import pl.delta.crm.calendar.CalendarEvent;

import java.time.Instant;
import java.util.List;

/**
 * Pełny termin — to, co widać w panelu szczegółów i w formularzu edycji.
 *
 * <p>{@code conflicts} to inne terminy tego samego agenta zachodzące na ten
 * zakres. Kolizja świadomie <b>nie blokuje</b> zapisu: agent bywa w dwóch
 * miejscach naraz z premedytacją (dzień otwarty i telefon w tle), a twarde 409
 * nauczyłoby go prowadzić terminarz obok systemu. Zamiast tego serwer mówi,
 * co się nakłada, a decyzję zostawia człowiekowi.
 */
public record EventResponse(
        EventSummary summary,
        String description,
        String outcomeNote,
        String createdByName,
        Instant createdAt,
        Instant updatedAt,
        List<EventSummary> conflicts
) {

    public static EventResponse from(CalendarEvent event, List<EventSummary> conflicts) {
        return new EventResponse(
                EventSummary.from(event),
                event.getDescription(),
                event.getOutcomeNote(),
                event.getCreatedBy().getFirstName() + " " + event.getCreatedBy().getLastName(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                conflicts
        );
    }
}
