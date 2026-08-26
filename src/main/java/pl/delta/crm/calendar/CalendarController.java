package pl.delta.crm.calendar;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import pl.delta.crm.calendar.dictionary.EventStatus;
import pl.delta.crm.calendar.dictionary.EventType;
import pl.delta.crm.calendar.dto.CreateEventRequest;
import pl.delta.crm.calendar.dto.EventResponse;
import pl.delta.crm.calendar.dto.EventSummary;
import pl.delta.crm.calendar.dto.UpdateEventStatusRequest;
import pl.delta.crm.security.AppUserPrincipal;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Kalendarz biura. Zakres widoczności bierze się z tokenu — identyfikatora biura
 * nie ma w żadnym parametrze, tak samo jak przy ofertach i klientach.
 *
 * <p>Lista terminów nie jest stronicowana, w odróżnieniu od ofert i klientów:
 * widok kalendarza z natury pyta o zamknięty przedział („ten tydzień"), a nie
 * o pierwsze 25 wpisów. Rozmiar odpowiedzi ogranicza więc zakres dat, którego
 * górną granicę pilnuje serwis.
 */
@RestController
@RequestMapping("/api/calendar/events")
public class CalendarController {

    private final CalendarEventService calendar;

    public CalendarController(CalendarEventService calendar) {
        this.calendar = calendar;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        EventResponse created = calendar.create(request, principal.user());
        URI location = UriComponentsBuilder.fromPath("/api/calendar/events/{id}")
                .buildAndExpand(created.summary().id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    /**
     * @param from  początek zakresu (ISO-8601, np. {@code 2026-08-01T00:00:00Z})
     * @param to    koniec zakresu, wyłączny
     * @param mine  zawęża do terminów zalogowanego agenta; wygodniejsze na froncie
     *              niż wstawianie tam własnego identyfikatora
     */
    @GetMapping
    public List<EventSummary> list(
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false, defaultValue = "false") boolean mine,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        UUID scopedAgent = mine ? principal.user().getId() : agentId;
        return calendar.list(principal.user(), from, to, scopedAgent, type, status);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable UUID id,
                             @AuthenticationPrincipal AppUserPrincipal principal) {
        return calendar.get(id, principal.user());
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable UUID id,
                                @Valid @RequestBody CreateEventRequest request,
                                @AuthenticationPrincipal AppUserPrincipal principal) {
        return calendar.update(id, request, principal.user());
    }

    /** Szybkie domknięcie terminu — bez przechodzenia przez cały formularz. */
    @PutMapping("/{id}/status")
    public EventResponse changeStatus(@PathVariable UUID id,
                                      @Valid @RequestBody UpdateEventStatusRequest request,
                                      @AuthenticationPrincipal AppUserPrincipal principal) {
        return calendar.changeStatus(id, request, principal.user());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        calendar.delete(id, principal.user());
        return ResponseEntity.noContent().build();
    }
}
