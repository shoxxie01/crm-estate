package pl.delta.crm.deal;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.delta.crm.deal.dto.CalendarDeadline;
import pl.delta.crm.security.AppUserPrincipal;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Terminy umowne widziane z kalendarza. Ścieżka należy do kalendarza, ale
 * właścicielem jest moduł transakcji. Tak jak {@code CalendarLinkController}
 * w drugą stronę. Kalendarz dociąga tę warstwę osobnym żądaniem i rysuje ją
 * obok terminów; nic nie jest kopiowane do {@code calendar_events}.
 */
@RestController
public class DeadlineCalendarController {

    private final DealDeadlineService deadlines;

    public DeadlineCalendarController(DealDeadlineService deadlines) {
        this.deadlines = deadlines;
    }

    /**
     * @param from pierwszy dzień zakresu (np. {@code 2026-10-01})
     * @param to   dzień po ostatnim. Zakres wyłączny, jak przy terminach
     */
    @GetMapping("/api/calendar/deadlines")
    public List<CalendarDeadline> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "false") boolean mine,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        UUID agentId = mine ? principal.user().getId() : null;
        return deadlines.forCalendar(principal.user(), from, to, agentId);
    }
}
