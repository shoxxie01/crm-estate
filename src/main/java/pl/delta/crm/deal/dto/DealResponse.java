package pl.delta.crm.deal.dto;

import pl.delta.crm.calendar.dto.EventSummary;
import pl.delta.crm.deal.Deal;
import pl.delta.crm.deal.DealStageChange;
import pl.delta.crm.deal.dictionary.DeadlineStatus;
import pl.delta.crm.deal.dictionary.DealStage;

import java.time.Instant;
import java.util.List;

/**
 * Pełna transakcja. Panel karty: pola, historia etapów, zainteresowani i wszystkie terminy,
 * które są jej krokami (z rezultatami, więc widać, jak przebiegała sprzedaż).
 */
public record DealResponse(
        DealCard card,
        String notes,
        String lostNote,
        String createdByName,
        Instant updatedAt,
        List<StageChangeEntry> history,
        List<EventSummary> events,
        List<InterestEntry> interests,
        /** Terminy umowne razem z przesuniętymi. Historia aneksów zostaje widoczna. */
        List<DeadlineEntry> deadlines
) {

    public record StageChangeEntry(DealStage fromStage, DealStage toStage,
                                   String changedByName, Instant changedAt) {

        static StageChangeEntry from(DealStageChange change) {
            return new StageChangeEntry(
                    change.getFromStage(),
                    change.getToStage(),
                    change.getChangedBy().getFirstName() + " " + change.getChangedBy().getLastName(),
                    change.getChangedAt());
        }
    }

    public static DealResponse from(Deal deal, EventSummary nextEvent, DealCard.InterestStats stats,
                                    List<DealStageChange> history, List<EventSummary> events,
                                    List<InterestEntry> interests, List<DeadlineEntry> deadlines) {
        // Lista jest posortowana po dacie; zamknięta transakcja nie ma już czego pilnować.
        DeadlineEntry nextDeadline = deal.getStage().isClosed() ? null : deadlines.stream()
                .filter(entry -> entry.status() == DeadlineStatus.OPEN)
                .findFirst()
                .orElse(null);
        return new DealResponse(
                DealCard.from(deal, nextEvent, stats, nextDeadline),
                deal.getNotes(),
                deal.getLostNote(),
                deal.getCreatedBy().getFirstName() + " " + deal.getCreatedBy().getLastName(),
                deal.getUpdatedAt(),
                history.stream().map(StageChangeEntry::from).toList(),
                events,
                interests,
                deadlines
        );
    }
}
