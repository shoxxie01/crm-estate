package pl.delta.crm.deal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.delta.crm.calendar.CalendarEvent;
import pl.delta.crm.calendar.CalendarEventRepository;
import pl.delta.crm.calendar.dictionary.EventStatus;
import pl.delta.crm.calendar.dto.EventSummary;
import pl.delta.crm.client.Client;
import pl.delta.crm.client.ClientRepository;
import pl.delta.crm.contact.PhoneNumber;
import pl.delta.crm.deal.dictionary.DealStage;
import pl.delta.crm.deal.dictionary.InterestStatus;
import pl.delta.crm.deal.dto.ChangeStageRequest;
import pl.delta.crm.deal.dto.DealCard;
import pl.delta.crm.deal.dto.DealRequest;
import pl.delta.crm.deal.dto.DealResponse;
import pl.delta.crm.deal.dto.DeadlineEntry;
import pl.delta.crm.deal.dto.InterestEntry;
import pl.delta.crm.deal.dictionary.DeadlineStatus;
import pl.delta.crm.deal.dto.InterestRequest;
import pl.delta.crm.error.BusinessValidationException;
import pl.delta.crm.error.ClientNotFoundException;
import pl.delta.crm.error.DealNotFoundException;
import pl.delta.crm.error.InterestNotFoundException;
import pl.delta.crm.error.PropertyNotFoundException;
import pl.delta.crm.property.Property;
import pl.delta.crm.property.PropertyRepository;
import pl.delta.crm.user.User;
import pl.delta.crm.user.UserRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class DealService {

    /**
     * Jak długo zamknięta karta zostaje na tablicy. Miesiąc wystarcza, żeby na
     * odprawie widać było świeże wygrane i przegrane, a kolumny nie puchną.
     */
    private static final Duration CLOSED_VISIBLE_FOR = Duration.ofDays(30);

    private static final Set<DealStage> CLOSED = EnumSet.of(DealStage.WON, DealStage.LOST);

    /** Termin, który jeszcze może się odbyć. Tylko taki jest „następnym krokiem". */
    private static final Set<EventStatus> UPCOMING = EnumSet.of(EventStatus.PLANNED, EventStatus.CONFIRMED);

    private final DealRepository deals;
    private final DealStageChangeRepository history;
    private final DealInterestRepository interests;
    private final DealDeadlineRepository deadlines;
    private final CalendarEventRepository events;
    private final PropertyRepository properties;
    private final ClientRepository clients;
    private final UserRepository users;

    public DealService(DealRepository deals,
                       DealStageChangeRepository history,
                       DealInterestRepository interests,
                       DealDeadlineRepository deadlines,
                       CalendarEventRepository events,
                       PropertyRepository properties,
                       ClientRepository clients,
                       UserRepository users) {
        this.deals = deals;
        this.history = history;
        this.interests = interests;
        this.deadlines = deadlines;
        this.events = events;
        this.properties = properties;
        this.clients = clients;
        this.users = users;
    }

    @Transactional
    public DealResponse create(DealRequest request, User author) {
        DealStage stage = request.stage() == null ? DealStage.LEAD : request.stage();
        if (stage.isClosed()) {
            throw new BusinessValidationException(
                    Map.of("stage", "Nowa transakcja zaczyna się w jednym z otwartych etapów."));
        }

        UUID agencyId = author.getAgency().getId();
        Property property = resolveProperty(request.propertyId(), agencyId);
        // Właściciel podpowiada się z oferty. Przepisywanie go ręcznie to ten sam
        // klient wybrany drugi raz.
        Client client = request.clientId() != null
                ? resolveClient(request.clientId(), agencyId)
                : property == null ? null : property.getOwner();

        ensureNoOtherOpenDeal(property, null);

        Deal deal = new Deal(
                author.getAgency(),
                resolveAgent(request.agentId(), author),
                author,
                resolveTitle(request.title(), property, client),
                stage);

        deal.setProperty(property);
        deal.setClient(client);
        // Pusta wartość = cena z oferty. Agent i tak wpisałby właśnie ją,
        // a różnica pojawia się dopiero w negocjacjach.
        deal.setValue(request.value() != null ? request.value()
                : property == null || property.getPricing() == null ? null : property.getPricing().getPrice());
        deal.setCommission(request.commission());
        deal.setNotes(trimToNull(request.notes()));

        Deal saved = deals.save(deal);
        history.save(new DealStageChange(saved, null, stage, author));
        return response(saved);
    }

    @Transactional
    public DealResponse update(UUID id, DealRequest request, User actor) {
        UUID agencyId = actor.getAgency().getId();
        Deal deal = find(id, agencyId);

        Property property = resolveProperty(request.propertyId(), agencyId);
        Client client = resolveClient(request.clientId(), agencyId);

        if (!deal.getStage().isClosed()) {
            ensureNoOtherOpenDeal(property, deal.getId());
        }

        deal.setTitle(resolveTitle(request.title(), property, client));
        deal.setProperty(property);
        deal.setClient(client);
        // Kupującego nie ustawia się w formularzu. Wynika z przyjętej oferty
        // na liście zainteresowanych (patrz applyInterest).
        deal.setValue(request.value());
        deal.setCommission(request.commission());
        deal.setNotes(trimToNull(request.notes()));
        if (request.agentId() != null) {
            deal.setAgent(resolveAgent(request.agentId(), actor));
        }

        return response(deals.save(deal));
    }

    /**
     * Przeniesienie karty. Każda faktyczna zmiana etapu trafia do historii;
     * „przeniesienie" do tej samej kolumny (np. poprawka powodu przegranej)
     * zmienia tylko pola, bez wpisu.
     */
    @Transactional
    public DealResponse changeStage(UUID id, ChangeStageRequest request, User actor) {
        Deal deal = find(id, actor.getAgency().getId());
        DealStage target = request.stage();

        if (target == DealStage.LOST && request.lostReason() == null) {
            throw new BusinessValidationException(
                    Map.of("lostReason", "Podaj powód przegranej. Bez niego nie da się wyciągnąć wniosków."));
        }
        // Karta wyciągana z „Wygranych"/„Przegranych" wraca do lejka, więc musi
        // spełnić tę samą regułę co nowa: jedna otwarta karta na ofertę.
        if (deal.getStage().isClosed() && !target.isClosed()) {
            ensureNoOtherOpenDeal(deal.getProperty(), deal.getId());
        }

        DealStage previous = deal.getStage();
        if (deal.moveTo(target, request.lostReason(), trimToNull(request.lostNote()))) {
            history.save(new DealStageChange(deal, previous, target, actor));
        }

        return response(deals.save(deal));
    }

    @Transactional
    public void delete(UUID id, User actor) {
        Deal deal = find(id, actor.getAgency().getId());
        // Historia etapów znika kaskadowo, terminy zostają w kalendarzu i tracą
        // tylko powiązanie (ON DELETE w V16). Usunięcie to sprzątanie pomyłek.
        // Transakcję, która się nie udała, zamyka się jako przegraną.
        deals.delete(deal);
    }

    @Transactional(readOnly = true)
    public DealResponse get(UUID id, User viewer) {
        return response(find(id, viewer.getAgency().getId()));
    }

    /**
     * Zawartość tablicy. Jak kalendarz. Wspólna dla biura; „tylko moje" to
     * filtr, nie uprawnienie, bo zastępstwo za agenta na urlopie wymaga
     * widzenia jego lejka.
     */
    @Transactional(readOnly = true)
    public List<DealCard> board(User viewer, UUID agentId) {
        Instant now = Instant.now();
        List<Deal> found = deals.findBoard(viewer.getAgency().getId(), CLOSED, now.minus(CLOSED_VISIBLE_FOR))
                .stream()
                .filter(deal -> agentId == null || deal.getAgent().getId().equals(agentId))
                .toList();

        Map<UUID, EventSummary> next = nextEvents(
                found.stream().filter(deal -> !deal.getStage().isClosed()).map(Deal::getId).toList(), now);

        Map<UUID, List<DealInterest>> byDeal = new HashMap<>();
        if (!found.isEmpty()) {
            for (DealInterest interest : interests.findByDeals(found.stream().map(Deal::getId).toList())) {
                byDeal.computeIfAbsent(interest.getDeal().getId(), key -> new ArrayList<>()).add(interest);
            }
        }

        // Lista jest posortowana po dacie, więc pierwszy otwarty termin karty jest najbliższy.
        Map<UUID, DeadlineEntry> nextDeadline = new HashMap<>();
        if (!found.isEmpty()) {
            for (DealDeadline deadline : deadlines.findOpenForDeals(
                    found.stream().map(Deal::getId).toList(), DeadlineStatus.OPEN)) {
                nextDeadline.putIfAbsent(deadline.getDeal().getId(), DeadlineEntry.from(deadline));
            }
        }

        return found.stream()
                .map(deal -> DealCard.from(deal, next.get(deal.getId()),
                        stats(byDeal.getOrDefault(deal.getId(), List.of())),
                        deal.getStage().isClosed() ? null : nextDeadline.get(deal.getId())))
                .toList();
    }

    // --- zainteresowani -------------------------------------------------------

    @Transactional
    public DealResponse addInterest(UUID dealId, InterestRequest request, User actor) {
        UUID agencyId = actor.getAgency().getId();
        Deal deal = find(dealId, agencyId);

        DealInterest interest = new DealInterest(deal);
        applyInterest(deal, interest, request, agencyId);
        interests.save(interest);
        return response(deals.save(deal));
    }

    @Transactional
    public DealResponse updateInterest(UUID dealId, UUID interestId, InterestRequest request, User actor) {
        UUID agencyId = actor.getAgency().getId();
        Deal deal = find(dealId, agencyId);
        DealInterest interest = interests.findByIdAndDealId(interestId, dealId)
                .orElseThrow(InterestNotFoundException::new);

        applyInterest(deal, interest, request, agencyId);
        interests.save(interest);
        return response(deals.save(deal));
    }

    @Transactional
    public DealResponse deleteInterest(UUID dealId, UUID interestId, User actor) {
        Deal deal = find(dealId, actor.getAgency().getId());
        DealInterest interest = interests.findByIdAndDealId(interestId, dealId)
                .orElseThrow(InterestNotFoundException::new);

        if (interest.getStatus() == InterestStatus.ACCEPTED) {
            releaseBuyer(deal, interest);
        }
        // Udział w terminach znika kaskadowo (V17). Same terminy zostają.
        interests.delete(interest);
        interests.flush();
        return response(deals.save(deal));
    }

    /**
     * Pola wspólne dla dodawania i edycji zainteresowanego.
     *
     * <p>Przyjęcie oferty ({@link InterestStatus#ACCEPTED}) ustawia kupującego
     * karty i Gdy podano kwotę wartość transakcji: od tej chwili lejek liczy
     * cenę uzgodnioną, a nie ofertową. Przyjęta może być tylko jedna oferta
     * (pilnuje tego też indeks {@code uq_deal_interests_accepted} w V17).
     */
    private void applyInterest(Deal deal, DealInterest interest, InterestRequest request, UUID agencyId) {
        Client client = resolveClient(request.clientId(), agencyId);
        String name = trimToNull(request.name());

        if (client == null && name == null) {
            throw new BusinessValidationException(
                    Map.of("name", "Wybierz klienta z bazy albo wpisz imię i nazwisko."));
        }
        if (client != null && deal.getClient() != null && client.getId().equals(deal.getClient().getId())) {
            throw new BusinessValidationException(
                    Map.of("clientId", "Właściciel nie może być zainteresowanym własną ofertą."));
        }

        List<DealInterest> others = interests.findByDeal(deal.getId()).stream()
                .filter(other -> !other.getId().equals(interest.getId()))
                .toList();
        if (client != null && others.stream().anyMatch(other ->
                other.getClient() != null && other.getClient().getId().equals(client.getId()))) {
            throw new BusinessValidationException(
                    Map.of("clientId", "Ten klient jest już na liście zainteresowanych."));
        }

        InterestStatus status = request.status() == null ? InterestStatus.NEW : request.status();
        if (status == InterestStatus.ACCEPTED
                && others.stream().anyMatch(other -> other.getStatus() == InterestStatus.ACCEPTED)) {
            throw new BusinessValidationException(
                    Map.of("status", "Inna oferta jest już przyjęta. Najpierw zmień jej status."));
        }

        // Cofnięcie przyjęcia (albo podmiana osoby na przyjętej ofercie) zwalnia
        // kupującego, zanim ewentualnie ustawimy nowego.
        if (interest.getStatus() == InterestStatus.ACCEPTED) {
            releaseBuyer(deal, interest);
        }

        interest.setClient(client);
        // Przy kliencie z bazy imię i telefon biorą się z jego karty. Kopia
        // rozjechałaby się przy pierwszej poprawce nazwiska.
        interest.setName(client == null ? name : null);
        interest.setPhone(client == null ? PhoneNumber.normalize(request.phone()) : null);
        interest.setStatus(status);
        interest.setOfferAmount(request.offerAmount());
        interest.setNote(trimToNull(request.note()));

        if (status == InterestStatus.ACCEPTED) {
            deal.setBuyer(client);
            if (request.offerAmount() != null) {
                deal.setValue(request.offerAmount());
            }
        }
    }

    /** Cofnięta albo usunięta przyjęta oferta zwalnia miejsce kupującego. */
    private void releaseBuyer(Deal deal, DealInterest interest) {
        if (deal.getBuyer() != null && interest.getClient() != null
                && deal.getBuyer().getId().equals(interest.getClient().getId())) {
            deal.setBuyer(null);
        }
    }

    /** Liczniki na kartę tablicy: aktywni zainteresowani, oferty, najwyższa kwota. */
    private static DealCard.InterestStats stats(List<DealInterest> list) {
        int active = (int) list.stream().filter(interest -> interest.getStatus().isActive()).count();
        int offers = (int) list.stream().filter(interest -> interest.getStatus().hasOffer()).count();
        BigDecimal best = list.stream()
                .filter(interest -> interest.getStatus().hasOffer() && interest.getOfferAmount() != null)
                .map(DealInterest::getOfferAmount)
                .max(Comparator.naturalOrder())
                .orElse(null);
        String accepted = list.stream()
                .filter(interest -> interest.getStatus() == InterestStatus.ACCEPTED)
                .map(DealInterest::displayName)
                .findFirst()
                .orElse(null);
        return new DealCard.InterestStats(active, offers, best, accepted);
    }

    // --- środek --------------------------------------------------------------

    private DealResponse response(Deal deal) {
        UUID agencyId = deal.getAgency().getId();
        List<EventSummary> linked = events.findByDeal(agencyId, deal.getId()).stream()
                .map(EventSummary::from)
                .toList();
        EventSummary next = deal.getStage().isClosed()
                ? null
                : nextEvents(List.of(deal.getId()), Instant.now()).get(deal.getId());

        List<DealInterest> found = interests.findByDeal(deal.getId());
        return DealResponse.from(deal, next, stats(found), history.findByDeal(deal.getId()), linked,
                found.stream().map(InterestEntry::from).toList(),
                deadlines.findByDeal(deal.getId()).stream().map(DeadlineEntry::from).toList());
    }

    /** Najbliższy aktywny termin każdej transakcji. Jednym zapytaniem dla całej tablicy. */
    private Map<UUID, EventSummary> nextEvents(List<UUID> dealIds, Instant now) {
        Map<UUID, EventSummary> result = new HashMap<>();
        if (dealIds.isEmpty()) {
            return result;
        }
        // Lista jest posortowana po początku, więc pierwszy termin danej
        // transakcji jest jej najbliższym krokiem.
        for (CalendarEvent event : events.findUpcomingForDeals(dealIds, now, UPCOMING)) {
            result.putIfAbsent(event.getDeal().getId(), EventSummary.from(event));
        }
        return result;
    }

    private Deal find(UUID id, UUID agencyId) {
        return deals.findByIdAndAgencyId(id, agencyId).orElseThrow(DealNotFoundException::new);
    }

    private void ensureNoOtherOpenDeal(Property property, UUID currentDealId) {
        if (property == null) {
            return;
        }
        boolean taken = deals.findOpenForProperty(property.getId(), CLOSED).stream()
                .anyMatch(other -> !other.getId().equals(currentDealId));
        if (taken) {
            throw new BusinessValidationException(
                    Map.of("propertyId", "Ta oferta ma już otwartą transakcję na tablicy."));
        }
    }

    /** Pusty tytuł składamy z adresu oferty albo nazwiska właściciela. Jak przy terminach. */
    private String resolveTitle(String requested, Property property, Client client) {
        String title = trimToNull(requested);
        if (title == null && property != null) {
            title = property.getAddress() == null ? null : trimToNull(property.getAddress().shortLine());
            if (title == null) {
                title = property.getTitle();
            }
        }
        if (title == null && client != null) {
            title = client.fullName();
        }
        if (title == null) {
            title = "Nowa transakcja";
        }
        return title.length() <= 160 ? title : title.substring(0, 160);
    }

    private Property resolveProperty(UUID propertyId, UUID agencyId) {
        if (propertyId == null) {
            return null;
        }
        return properties.findByIdAndAgencyId(propertyId, agencyId)
                .orElseThrow(PropertyNotFoundException::new);
    }

    private Client resolveClient(UUID clientId, UUID agencyId) {
        if (clientId == null) {
            return null;
        }
        return clients.findByIdAndAgencyId(clientId, agencyId)
                .orElseThrow(ClientNotFoundException::new);
    }

    /** Transakcję można przekazać koledze, ale tylko z tego samego biura. */
    private User resolveAgent(UUID agentId, User author) {
        if (agentId == null || agentId.equals(author.getId())) {
            return author;
        }

        return users.findById(agentId)
                .filter(candidate -> Objects.equals(
                        candidate.getAgency().getId(), author.getAgency().getId()))
                .orElseThrow(() -> new BusinessValidationException(
                        Map.of("agentId", "Wybrany agent nie należy do tego biura.")));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
