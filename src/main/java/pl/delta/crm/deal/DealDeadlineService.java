package pl.delta.crm.deal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.delta.crm.deal.dictionary.DeadlineStatus;
import pl.delta.crm.deal.dto.CalendarDeadline;
import pl.delta.crm.deal.dto.DealResponse;
import pl.delta.crm.deal.dto.DeadlineRequest;
import pl.delta.crm.error.BusinessValidationException;
import pl.delta.crm.error.DealNotFoundException;
import pl.delta.crm.error.DeadlineNotFoundException;
import pl.delta.crm.user.User;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Terminy umowne transakcji. Każda zmiana zwraca całą kartę. Termin wpływa
 * na plakietkę na tablicy, więc panel i tak musiałby ją dociągnąć.
 */
@Service
public class DealDeadlineService {

    /** Najdłuższy zakres, o jaki pyta kalendarz. Jak przy terminach (MAX_RANGE). */
    private static final long MAX_RANGE_DAYS = 200;

    private final DealDeadlineRepository deadlines;
    private final DealRepository deals;
    private final DealService dealService;

    public DealDeadlineService(DealDeadlineRepository deadlines, DealRepository deals, DealService dealService) {
        this.deadlines = deadlines;
        this.deals = deals;
        this.dealService = dealService;
    }

    @Transactional
    public DealResponse add(UUID dealId, DeadlineRequest request, User actor) {
        Deal deal = findDeal(dealId, actor);
        if (request.type() == null) {
            throw new BusinessValidationException(Map.of("type", "Wybierz rodzaj terminu."));
        }

        DealDeadline deadline = new DealDeadline(deal, request.type(), request.dueDate(), null);
        deadline.setNote(trimToNull(request.note()));
        deadlines.save(deadline);
        return dealService.get(dealId, actor);
    }

    /** Poprawka pomyłki (zła data przy wpisywaniu). Bez śladu w historii, w odróżnieniu od przesunięcia. */
    @Transactional
    public DealResponse update(UUID dealId, UUID deadlineId, DeadlineRequest request, User actor) {
        DealDeadline deadline = find(dealId, deadlineId, actor);
        if (request.type() != null) {
            deadline.setType(request.type());
        }
        deadline.setDueDate(request.dueDate());
        deadline.setNote(trimToNull(request.note()));
        deadlines.save(deadline);
        return dealService.get(dealId, actor);
    }

    @Transactional
    public DealResponse markMet(UUID dealId, UUID deadlineId, User actor) {
        DealDeadline deadline = find(dealId, deadlineId, actor);
        requireOpen(deadline);
        deadline.markMet();
        deadlines.save(deadline);
        return dealService.get(dealId, actor);
    }

    @Transactional
    public DealResponse reopen(UUID dealId, UUID deadlineId, User actor) {
        DealDeadline deadline = find(dealId, deadlineId, actor);
        if (deadline.getStatus() != DeadlineStatus.MET) {
            throw new BusinessValidationException(
                    Map.of("status", "Przywrócić można tylko termin oznaczony jako dotrzymany."));
        }
        deadline.reopen();
        deadlines.save(deadline);
        return dealService.get(dealId, actor);
    }

    /**
     * Przesunięcie terminu (aneks). Stara data zostaje jako „przesunięty",
     * nowa powstaje obok i wskazuje na nią. Na karcie widać, ile razy
     * przesuwano np. termin aktu.
     */
    @Transactional
    public DealResponse move(UUID dealId, UUID deadlineId, DeadlineRequest request, User actor) {
        DealDeadline previous = find(dealId, deadlineId, actor);
        requireOpen(previous);
        if (request.dueDate().equals(previous.getDueDate())) {
            throw new BusinessValidationException(Map.of("dueDate", "Nowa data musi być inna niż dotychczasowa."));
        }

        previous.markMoved();
        deadlines.save(previous);

        DealDeadline moved = new DealDeadline(previous.getDeal(), previous.getType(), request.dueDate(), previous);
        moved.setNote(trimToNull(request.note()) != null ? trimToNull(request.note()) : previous.getNote());
        deadlines.save(moved);
        return dealService.get(dealId, actor);
    }

    @Transactional
    public DealResponse delete(UUID dealId, UUID deadlineId, User actor) {
        DealDeadline deadline = find(dealId, deadlineId, actor);
        deadlines.delete(deadline);
        deadlines.flush();
        return dealService.get(dealId, actor);
    }

    /**
     * Warstwa terminów umownych w kalendarzu. „Tylko moje" to terminy
     * transakcji prowadzonych przez agenta. Terminy nie mają własnego właściciela.
     */
    @Transactional(readOnly = true)
    public List<CalendarDeadline> forCalendar(User viewer, LocalDate from, LocalDate to, UUID agentId) {
        if (!to.isAfter(from)) {
            throw new BusinessValidationException(Map.of("to", "Koniec zakresu musi być późniejszy niż początek."));
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new BusinessValidationException(Map.of("to", "Zakres jest zbyt szeroki. Pytaj najwyżej o pół roku naraz."));
        }

        return deadlines.findInRange(viewer.getAgency().getId(), from, to, DeadlineStatus.MOVED).stream()
                .filter(deadline -> agentId == null || deadline.getDeal().getAgent().getId().equals(agentId))
                .map(CalendarDeadline::from)
                .toList();
    }

    private Deal findDeal(UUID dealId, User actor) {
        return deals.findByIdAndAgencyId(dealId, actor.getAgency().getId())
                .orElseThrow(DealNotFoundException::new);
    }

    private DealDeadline find(UUID dealId, UUID deadlineId, User actor) {
        findDeal(dealId, actor);
        return deadlines.findByIdAndDealId(deadlineId, dealId).orElseThrow(DeadlineNotFoundException::new);
    }

    private static void requireOpen(DealDeadline deadline) {
        if (deadline.getStatus() != DeadlineStatus.OPEN) {
            throw new BusinessValidationException(
                    Map.of("status", "Ten termin jest już rozstrzygnięty."));
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
