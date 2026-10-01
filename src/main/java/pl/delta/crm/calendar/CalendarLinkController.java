package pl.delta.crm.calendar;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pl.delta.crm.calendar.dto.EventSummary;
import pl.delta.crm.security.AppUserPrincipal;

import java.util.List;
import java.util.UUID;

/**
 * Terminy widziane od strony oferty i od strony klienta. Sekcja „Terminy" na
 * ich kartach.
 *
 * <p>Ścieżki należą do tamtych zasobów, ale kontroler mieszka w module
 * kalendarza i to on jest ich właścicielem. Dzięki temu dołożenie kalendarza nie
 * wymagało dopisania pola do {@code PropertyResponse} ani {@code ClientResponse}:
 * karta oferty i tak dociąga terminy osobnym żądaniem (są pod zakładką), a
 * wpięcie ich w odpowiedź oferty obciążyłoby każdy jej odczyt. Również te
 * z listy i z eksportu, gdzie terminy nikogo nie interesują.
 */
@RestController
public class CalendarLinkController {

    private final CalendarEventService calendar;

    public CalendarLinkController(CalendarEventService calendar) {
        this.calendar = calendar;
    }

    @GetMapping("/api/properties/{propertyId}/events")
    public List<EventSummary> forProperty(@PathVariable UUID propertyId,
                                          @AuthenticationPrincipal AppUserPrincipal principal) {
        return calendar.forProperty(propertyId, principal.user());
    }

    @GetMapping("/api/clients/{clientId}/events")
    public List<EventSummary> forClient(@PathVariable UUID clientId,
                                        @AuthenticationPrincipal AppUserPrincipal principal) {
        return calendar.forClient(clientId, principal.user());
    }
}
