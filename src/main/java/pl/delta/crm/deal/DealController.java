package pl.delta.crm.deal;

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
import pl.delta.crm.deal.dictionary.DealLostReason;
import pl.delta.crm.deal.dictionary.DealStage;
import pl.delta.crm.deal.dictionary.DeadlineStatus;
import pl.delta.crm.deal.dictionary.DeadlineType;
import pl.delta.crm.deal.dictionary.InterestStatus;
import pl.delta.crm.deal.dto.ChangeStageRequest;
import pl.delta.crm.deal.dto.DealCard;
import pl.delta.crm.deal.dto.DealReport;
import pl.delta.crm.deal.dto.DealRequest;
import pl.delta.crm.deal.dto.DealResponse;
import pl.delta.crm.deal.dto.DeadlineRequest;
import pl.delta.crm.deal.dto.InterestRequest;
import pl.delta.crm.property.dto.DictionaryEntry;
import pl.delta.crm.security.AppUserPrincipal;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tablica Kanban transakcji. Lista nie jest stronicowana: tablica pokazuje
 * wszystkie otwarte karty biura naraz (inaczej sumy kolumn kłamałyby),
 * a zamknięte ogranicza serwis do ostatniego miesiąca.
 */
@RestController
@RequestMapping("/api/deals")
public class DealController {

    private final DealService deals;
    private final DealReportService reports;
    private final DealDeadlineService deadlines;

    public DealController(DealService deals, DealReportService reports, DealDeadlineService deadlines) {
        this.deals = deals;
        this.reports = reports;
        this.deadlines = deadlines;
    }

    /** Słowniki tablicy. Kolejność etapów to kolejność kolumn. */
    @GetMapping("/dictionaries")
    public Map<String, Object> dictionaries() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stage", DictionaryEntry.of(DealStage.class));
        result.put("lostReason", DictionaryEntry.of(DealLostReason.class));
        result.put("interestStatus", DictionaryEntry.of(InterestStatus.class));
        result.put("deadlineType", DictionaryEntry.of(DeadlineType.class));
        result.put("deadlineStatus", DictionaryEntry.of(DeadlineStatus.class));
        return result;
    }

    @GetMapping
    public List<DealCard> board(
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false, defaultValue = "false") boolean mine,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        UUID scopedAgent = mine ? principal.user().getId() : agentId;
        return deals.board(principal.user(), scopedAgent);
    }

    /**
     * Raport lejka. Okres wyznacza kohortę transakcji założonych w tym czasie;
     * domyślnie ostatnie 90 dni.
     */
    @GetMapping("/report")
    public DealReport report(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false, defaultValue = "false") boolean mine,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(Duration.ofDays(90)) : from;
        UUID scopedAgent = mine ? principal.user().getId() : agentId;
        return reports.report(principal.user(), start, end, scopedAgent);
    }

    @PostMapping
    public ResponseEntity<DealResponse> create(
            @Valid @RequestBody DealRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        DealResponse created = deals.create(request, principal.user());
        URI location = UriComponentsBuilder.fromPath("/api/deals/{id}")
                .buildAndExpand(created.card().id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public DealResponse get(@PathVariable UUID id,
                            @AuthenticationPrincipal AppUserPrincipal principal) {
        return deals.get(id, principal.user());
    }

    @PutMapping("/{id}")
    public DealResponse update(@PathVariable UUID id,
                               @Valid @RequestBody DealRequest request,
                               @AuthenticationPrincipal AppUserPrincipal principal) {
        return deals.update(id, request, principal.user());
    }

    /** Przeciągnięcie karty do innej kolumny. */
    @PutMapping("/{id}/stage")
    public DealResponse changeStage(@PathVariable UUID id,
                                    @Valid @RequestBody ChangeStageRequest request,
                                    @AuthenticationPrincipal AppUserPrincipal principal) {
        return deals.changeStage(id, request, principal.user());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        deals.delete(id, principal.user());
        return ResponseEntity.noContent().build();
    }

    // --- zainteresowani ------------------------------------------------------
    // Każda operacja zwraca całą kartę: zmiana statusu zainteresowanego potrafi
    // przestawić kupującego i wartość transakcji, więc panel i tak musiałby ją
    // dociągnąć od nowa.

    @PostMapping("/{id}/interests")
    public DealResponse addInterest(@PathVariable UUID id,
                                    @Valid @RequestBody InterestRequest request,
                                    @AuthenticationPrincipal AppUserPrincipal principal) {
        return deals.addInterest(id, request, principal.user());
    }

    @PutMapping("/{id}/interests/{interestId}")
    public DealResponse updateInterest(@PathVariable UUID id,
                                       @PathVariable UUID interestId,
                                       @Valid @RequestBody InterestRequest request,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        return deals.updateInterest(id, interestId, request, principal.user());
    }

    @DeleteMapping("/{id}/interests/{interestId}")
    public DealResponse deleteInterest(@PathVariable UUID id,
                                       @PathVariable UUID interestId,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        return deals.deleteInterest(id, interestId, principal.user());
    }

    // --- terminy umowne ------------------------------------------------------

    @PostMapping("/{id}/deadlines")
    public DealResponse addDeadline(@PathVariable UUID id,
                                    @Valid @RequestBody DeadlineRequest request,
                                    @AuthenticationPrincipal AppUserPrincipal principal) {
        return deadlines.add(id, request, principal.user());
    }

    /** Poprawka pomyłki. Zmiana daty wynikająca z aneksu idzie przez {@code /move}. */
    @PutMapping("/{id}/deadlines/{deadlineId}")
    public DealResponse updateDeadline(@PathVariable UUID id,
                                       @PathVariable UUID deadlineId,
                                       @Valid @RequestBody DeadlineRequest request,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        return deadlines.update(id, deadlineId, request, principal.user());
    }

    @PutMapping("/{id}/deadlines/{deadlineId}/met")
    public DealResponse markDeadlineMet(@PathVariable UUID id,
                                        @PathVariable UUID deadlineId,
                                        @AuthenticationPrincipal AppUserPrincipal principal) {
        return deadlines.markMet(id, deadlineId, principal.user());
    }

    @PutMapping("/{id}/deadlines/{deadlineId}/reopen")
    public DealResponse reopenDeadline(@PathVariable UUID id,
                                       @PathVariable UUID deadlineId,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        return deadlines.reopen(id, deadlineId, principal.user());
    }

    @PutMapping("/{id}/deadlines/{deadlineId}/move")
    public DealResponse moveDeadline(@PathVariable UUID id,
                                     @PathVariable UUID deadlineId,
                                     @Valid @RequestBody DeadlineRequest request,
                                     @AuthenticationPrincipal AppUserPrincipal principal) {
        return deadlines.move(id, deadlineId, request, principal.user());
    }

    @DeleteMapping("/{id}/deadlines/{deadlineId}")
    public DealResponse deleteDeadline(@PathVariable UUID id,
                                       @PathVariable UUID deadlineId,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        return deadlines.delete(id, deadlineId, principal.user());
    }
}
