package pl.delta.crm.calendar;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.delta.crm.calendar.dictionary.EventOutcome;
import pl.delta.crm.calendar.dictionary.EventStatus;
import pl.delta.crm.calendar.dictionary.EventType;
import pl.delta.crm.calendar.dto.CreateEventRequest;
import pl.delta.crm.calendar.dto.EventResponse;
import pl.delta.crm.calendar.dto.EventSummary;
import pl.delta.crm.calendar.dto.UpdateEventStatusRequest;
import pl.delta.crm.client.Client;
import pl.delta.crm.client.ClientRepository;
import pl.delta.crm.contact.PhoneNumber;
import pl.delta.crm.deal.Deal;
import pl.delta.crm.deal.DealInterest;
import pl.delta.crm.deal.DealInterestRepository;
import pl.delta.crm.deal.dictionary.InterestStatus;
import pl.delta.crm.deal.DealRepository;
import pl.delta.crm.error.BusinessValidationException;
import pl.delta.crm.error.CalendarEventNotFoundException;
import pl.delta.crm.error.ClientNotFoundException;
import pl.delta.crm.error.DealNotFoundException;
import pl.delta.crm.error.InterestNotFoundException;
import pl.delta.crm.error.PropertyNotFoundException;
import pl.delta.crm.property.Property;
import pl.delta.crm.property.PropertyRepository;
import pl.delta.crm.user.User;
import pl.delta.crm.user.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class CalendarEventService {

    /**
     * Najdłuższy zakres, o jaki wolno zapytać jednym żądaniem. Widok kalendarza
     * potrzebuje najwyżej miesiąca z zapasem; bez limitu pierwsze `from=1970`
     * ściągnęłoby całą tabelę biura razem z ofertami i klientami.
     */
    private static final Duration MAX_RANGE = Duration.ofDays(200);

    /**
     * Rodzaje terminów, po których zainteresowany na pewno widział ofertę.
     * Odbyta prezentacja przestawia jego status z „zgłosił się" na „oglądał".
     */
    private static final Set<EventType> VIEWINGS = EnumSet.of(EventType.PRESENTATION, EventType.OPEN_HOUSE);

    private final CalendarEventRepository events;
    private final PropertyRepository properties;
    private final ClientRepository clients;
    private final DealRepository deals;
    private final DealInterestRepository interests;
    private final UserRepository users;

    public CalendarEventService(CalendarEventRepository events,
                                PropertyRepository properties,
                                ClientRepository clients,
                                DealRepository deals,
                                DealInterestRepository interests,
                                UserRepository users) {
        this.events = events;
        this.properties = properties;
        this.clients = clients;
        this.deals = deals;
        this.interests = interests;
        this.users = users;
    }

    @Transactional
    public EventResponse create(CreateEventRequest request, User author) {
        validateRange(request.startsAt(), request.endsAt());
        validateOutcome(request.status(), request.outcome());

        UUID agencyId = author.getAgency().getId();
        Property property = resolveProperty(request.propertyId(), agencyId);
        Client client = resolveClient(request.clientId(), agencyId);
        Deal deal = resolveDeal(request.dealId(), agencyId);
        User agent = resolveAgent(request.agentId(), author);

        CalendarEvent event = new CalendarEvent(
                author.getAgency(),
                agent,
                author,
                request.type(),
                resolveTitle(request, property, client),
                request.startsAt(),
                request.endsAt());

        apply(event, request, property, client, deal);

        CalendarEvent saved = events.save(event);
        return EventResponse.from(saved, conflictsFor(saved));
    }

    @Transactional
    public EventResponse update(UUID id, CreateEventRequest request, User actor) {
        UUID agencyId = actor.getAgency().getId();
        CalendarEvent event = events.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(CalendarEventNotFoundException::new);

        validateRange(request.startsAt(), request.endsAt());
        validateOutcome(request.status(), request.outcome());

        Property property = resolveProperty(request.propertyId(), agencyId);
        Client client = resolveClient(request.clientId(), agencyId);
        Deal deal = resolveDeal(request.dealId(), agencyId);

        event.setType(request.type());
        event.setTitle(resolveTitle(request, property, client));
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());

        // Pusty agentId przy edycji = nie zmieniaj właściciela terminu (jak przy kliencie).
        if (request.agentId() != null) {
            event.setAgent(resolveAgent(request.agentId(), actor));
        }

        apply(event, request, property, client, deal);

        CalendarEvent saved = events.save(event);
        return EventResponse.from(saved, conflictsFor(saved));
    }

    /** Domknięcie terminu: status, rezultat i notatka. Bez ruszania reszty wpisu. */
    @Transactional
    public EventResponse changeStatus(UUID id, UpdateEventStatusRequest request, User actor) {
        CalendarEvent event = events.findByIdAndAgencyId(id, actor.getAgency().getId())
                .orElseThrow(CalendarEventNotFoundException::new);

        validateOutcome(request.status(), request.outcome());

        event.setStatus(request.status());
        // Rezultat i notatka mają sens tylko przy odbytym terminie. Przy każdym
        // innym statusie czyścimy je, żeby nie zostały po cofniętym „odbyło się".
        boolean happened = request.status() == EventStatus.COMPLETED;
        event.setOutcome(happened ? request.outcome() : null);
        event.setOutcomeNote(happened ? trimToNull(request.outcomeNote()) : null);
        if (happened) {
            markViewed(event);
        }

        CalendarEvent saved = events.save(event);
        return EventResponse.from(saved, conflictsFor(saved));
    }

    @Transactional
    public void delete(UUID id, User actor) {
        CalendarEvent event = events.findByIdAndAgencyId(id, actor.getAgency().getId())
                .orElseThrow(CalendarEventNotFoundException::new);
        events.delete(event);
    }

    @Transactional(readOnly = true)
    public EventResponse get(UUID id, User viewer) {
        CalendarEvent event = events.findByIdAndAgencyId(id, viewer.getAgency().getId())
                .orElseThrow(CalendarEventNotFoundException::new);
        return EventResponse.from(event, conflictsFor(event));
    }

    /**
     * Terminy biura w zakresie dat. Kalendarz jest wspólny. Każdy w biurze widzi
     * wszystkie wpisy, a zawężenie do siebie to filtr, nie uprawnienie. Inaczej
     * nie dałoby się umówić zastępstwa ani sprawdzić, czy ktoś już nie jedzie
     * pod ten adres.
     *
     * <p>Filtry rodzaju i statusu działają na wyniku zapytania zakresowego, a nie
     * w SQL-u: przy jednym miesiącu to kilkadziesiąt rekordów, a alternatywą
     * byłoby sześć wariantów zapytania albo warunki `:param is null` w JPQL.
     */
    @Transactional(readOnly = true)
    public List<EventSummary> list(User viewer, Instant from, Instant to,
                                   UUID agentId, EventType type, EventStatus status) {

        validateQueryRange(from, to);

        return events.findInRange(viewer.getAgency().getId(), from, to).stream()
                .filter(event -> agentId == null || event.getAgent().getId().equals(agentId))
                .filter(event -> type == null || event.getType() == type)
                .filter(event -> status == null || event.getStatus() == status)
                .map(EventSummary::from)
                .toList();
    }

    /** Historia i plany dotyczące jednej oferty. Sekcja „Terminy" na jej karcie. */
    @Transactional(readOnly = true)
    public List<EventSummary> forProperty(UUID propertyId, User viewer) {
        UUID agencyId = viewer.getAgency().getId();
        properties.findByIdAndAgencyId(propertyId, agencyId)
                .orElseThrow(PropertyNotFoundException::new);

        return events.findByProperty(agencyId, propertyId).stream()
                .map(EventSummary::from)
                .toList();
    }

    /** To samo dla klienta. Co się z nim działo i co jest umówione. */
    @Transactional(readOnly = true)
    public List<EventSummary> forClient(UUID clientId, User viewer) {
        UUID agencyId = viewer.getAgency().getId();
        clients.findByIdAndAgencyId(clientId, agencyId)
                .orElseThrow(ClientNotFoundException::new);

        return events.findByClient(agencyId, clientId).stream()
                .map(EventSummary::from)
                .toList();
    }

    // --- środek --------------------------------------------------------------

    /** Pola wspólne dla dodawania i edycji. */
    private void apply(CalendarEvent event, CreateEventRequest request,
                       Property property, Client client, Deal deal) {

        event.setStatus(orDefault(request.status(), EventStatus.PLANNED));
        event.setDescription(trimToNull(request.description()));
        event.setAllDay(Boolean.TRUE.equals(request.allDay()));
        event.setProperty(property);
        event.setClient(client);
        event.setDeal(deal);
        event.setParticipants(resolveParticipants(request.participantIds(), deal));
        event.setCounterpartyName(trimToNull(request.counterpartyName()));
        event.setCounterpartyPhone(PhoneNumber.normalize(request.counterpartyPhone()));

        // Miejsce przepisane ręcznie, gdy jest identyczne z adresem oferty, tylko
        // rozjedzie się przy korekcie tej oferty. W takim wypadku zostawiamy puste
        // i pokazujemy adres z powiązania (EventSummary#displayLocation).
        String location = trimToNull(request.location());
        if (property != null && location != null
                && location.equalsIgnoreCase(property.getAddress().shortLine())) {
            location = null;
        }
        event.setLocation(location);

        boolean happened = event.getStatus() == EventStatus.COMPLETED;
        event.setOutcome(happened ? request.outcome() : null);
        event.setOutcomeNote(happened ? trimToNull(request.outcomeNote()) : null);
        if (happened) {
            markViewed(event);
        }
    }

    /**
     * Odbyta prezentacja przesuwa uczestników ze „zgłosił się" na „oglądał".
     * Tylko w przód: kogoś, kto już złożył ofertę albo odpadł, nie cofamy.
     */
    private void markViewed(CalendarEvent event) {
        if (!VIEWINGS.contains(event.getType())) {
            return;
        }
        for (DealInterest participant : event.getParticipants()) {
            if (participant.getStatus() == InterestStatus.NEW) {
                participant.setStatus(InterestStatus.VIEWED);
            }
        }
    }

    /** Uczestnicy muszą należeć do transakcji, której krokiem jest termin. */
    private Set<DealInterest> resolveParticipants(List<UUID> ids, Deal deal) {
        Set<DealInterest> result = new LinkedHashSet<>();
        if (ids == null || ids.isEmpty()) {
            return result;
        }
        if (deal == null) {
            throw new BusinessValidationException(
                    Map.of("participantIds", "Uczestników można wybrać tylko przy terminie powiązanym z transakcją."));
        }
        for (UUID id : ids) {
            result.add(interests.findByIdAndDealId(id, deal.getId())
                    .orElseThrow(InterestNotFoundException::new));
        }
        return result;
    }

    /**
     * Tytuł jest opcjonalny. Pusty składamy z rodzaju i kontekstu, bo „Prezentacja
     *. Grzybowska 41" to dokładnie to, co agent i tak by wpisał. Ucinamy do
     * długości kolumny; ucięcie dotyczy wyłącznie tytułu wygenerowanego, bo
     * wpisany ręcznie pilnuje już walidacja @Size.
     */
    private String resolveTitle(CreateEventRequest request, Property property, Client client) {
        String requested = trimToNull(request.title());
        if (requested != null) {
            return requested;
        }

        String context = null;
        if (property != null) {
            context = property.getAddress().shortLine();
            if (context == null) {
                context = property.getTitle();
            }
        } else if (client != null) {
            context = client.fullName();
        } else if (trimToNull(request.counterpartyName()) != null) {
            context = request.counterpartyName().trim();
        }

        String label = request.type().label();
        String title = context == null ? label : label + ". " + context;
        return title.length() <= 120 ? title : title.substring(0, 120);
    }

    /**
     * Kolizje w kalendarzu agenta i w tej samej ofercie. Nie blokują zapisu. Patrz komentarz przy
     * {@link EventResponse}. Zdarzenia całodniowe są z tego wyłączone: urlop albo
     * dzień otwarty nakładałby się wtedy na każdy termin tego dnia i ostrzeżenie
     * przestałoby cokolwiek znaczyć.
     */
    private List<EventSummary> conflictsFor(CalendarEvent event) {
        if (event.isAllDay() || !event.getStatus().blocksTime()) {
            return List.of();
        }

        // Mapa po identyfikatorze: termin tego samego agenta w tej samej ofercie
        // wpadłby do obu list, a ostrzeżenie ma go pokazać raz.
        Map<UUID, CalendarEvent> found = new LinkedHashMap<>();
        for (CalendarEvent other : events.findOverlappingForAgent(
                event.getAgent().getId(), event.getStartsAt(), event.getEndsAt(), EventStatus.CANCELLED)) {
            found.putIfAbsent(other.getId(), other);
        }
        if (event.getProperty() != null) {
            for (CalendarEvent other : events.findOverlappingForProperty(
                    event.getProperty().getId(), event.getStartsAt(), event.getEndsAt(), EventStatus.CANCELLED)) {
                found.putIfAbsent(other.getId(), other);
            }
        }

        return found.values().stream()
                .filter(other -> !other.getId().equals(event.getId()))
                .filter(other -> !other.isAllDay())
                .map(EventSummary::from)
                .toList();
    }

    private void validateRange(Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new BusinessValidationException(
                    Map.of("endsAt", "Koniec terminu musi być późniejszy niż początek."));
        }
    }

    private void validateQueryRange(Instant from, Instant to) {
        if (!to.isAfter(from)) {
            throw new BusinessValidationException(
                    Map.of("to", "Koniec zakresu musi być późniejszy niż początek."));
        }
        if (Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
            throw new BusinessValidationException(
                    Map.of("to", "Zakres jest zbyt szeroki. Pytaj najwyżej o pół roku naraz."));
        }
    }

    /** Rezultat bez odbytego terminu byłby wróżeniem. Pilnuje tego też CHECK w V10. */
    private void validateOutcome(EventStatus status, EventOutcome outcome) {
        if (outcome != null && status != EventStatus.COMPLETED) {
            throw new BusinessValidationException(
                    Map.of("outcome", "Rezultat można podać dopiero dla terminu, który się odbył."));
        }
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

    private Deal resolveDeal(UUID dealId, UUID agencyId) {
        if (dealId == null) {
            return null;
        }
        return deals.findByIdAndAgencyId(dealId, agencyId)
                .orElseThrow(DealNotFoundException::new);
    }

    /** Termin można wpisać koledze z biura, ale tylko z tego samego biura. */
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

    private static <T> T orDefault(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
