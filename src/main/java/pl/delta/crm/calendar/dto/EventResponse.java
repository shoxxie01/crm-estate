package pl.delta.crm.calendar.dto;

import pl.delta.crm.calendar.CalendarEvent;
import pl.delta.crm.calendar.dictionary.EventStatus;
import pl.delta.crm.deal.Deal;
import pl.delta.crm.deal.DealStageAdvisor;
import pl.delta.crm.deal.dictionary.DealStage;
import pl.delta.crm.deal.dto.InterestEntry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Pełny termin. To, co widać w panelu szczegółów i w formularzu edycji.
 *
 * <p>{@code conflicts} to inne terminy tego samego agenta zachodzące na ten
 * zakres. Oraz terminy innych agentów w tej samej ofercie, bo dwie ekipy
 * pod jednymi drzwiami o tej samej godzinie to też kolizja. Kolizja świadomie
 * <b>nie blokuje</b> zapisu: agent bywa w dwóch miejscach naraz z premedytacją
 * (dzień otwarty i telefon w tle), a twarde 409 nauczyłoby go prowadzić
 * terminarz obok systemu. Zamiast tego serwer mówi, co się nakłada, a decyzję
 * zostawia człowiekowi.
 *
 * <p>{@code suggestion} działa w tym samym duchu: po odbytym terminie serwer
 * podpowiada, do którego etapu przenieść powiązaną transakcję (patrz
 * {@link DealStageAdvisor}), ale niczego sam nie przesuwa. Propozycja jest
 * liczona przy każdym odczycie, więc znika sama, gdy karta jest już dalej.
 */
public record EventResponse(
        EventSummary summary,
        String description,
        String outcomeNote,
        String createdByName,
        Instant createdAt,
        Instant updatedAt,
        List<EventSummary> conflicts,
        /** Zainteresowani z transakcji obecni na terminie, z ich bieżącym statusem. */
        List<InterestEntry> participants,
        StageSuggestion suggestion
) {

    /** Propozycja przeniesienia karty transakcji po rezultacie terminu. */
    public record StageSuggestion(UUID dealId, String dealTitle,
                                  DealStage fromStage, DealStage toStage, String reason) {
    }

    public static EventResponse from(CalendarEvent event, List<EventSummary> conflicts) {
        return new EventResponse(
                EventSummary.from(event),
                event.getDescription(),
                event.getOutcomeNote(),
                event.getCreatedBy().getFirstName() + " " + event.getCreatedBy().getLastName(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                conflicts,
                event.getParticipants().stream().map(InterestEntry::from).toList(),
                suggestionFor(event)
        );
    }

    private static StageSuggestion suggestionFor(CalendarEvent event) {
        Deal deal = event.getDeal();
        if (deal == null || event.getStatus() != EventStatus.COMPLETED) {
            return null;
        }
        return DealStageAdvisor.advise(event.getType(), event.getOutcome(), deal.getStage())
                .map(advice -> new StageSuggestion(
                        deal.getId(), deal.getTitle(), deal.getStage(), advice.stage(), advice.reason()))
                .orElse(null);
    }
}
