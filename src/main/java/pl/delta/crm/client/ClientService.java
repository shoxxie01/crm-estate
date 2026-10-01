package pl.delta.crm.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.delta.crm.calendar.CalendarEventRepository;
import pl.delta.crm.client.dictionary.ClientStatus;
import pl.delta.crm.client.dictionary.RequirementStatus;
import pl.delta.crm.client.dto.ClientResponse;
import pl.delta.crm.client.dto.ClientSummary;
import pl.delta.crm.client.dto.CreateClientRequest;
import pl.delta.crm.client.requirement.ClientRequirementRepository;
import pl.delta.crm.client.requirement.RequirementTransactionCount;
import pl.delta.crm.contact.PhoneNumber;
import pl.delta.crm.error.BusinessValidationException;
import pl.delta.crm.error.ClientNotFoundException;
import pl.delta.crm.error.PropertyNotFoundException;
import pl.delta.crm.property.OwnerTransactionCount;
import pl.delta.crm.property.Property;
import pl.delta.crm.property.PropertyRepository;
import pl.delta.crm.property.dictionary.TransactionType;
import pl.delta.crm.search.SearchTerms;
import pl.delta.crm.user.User;
import pl.delta.crm.user.UserRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClientService {

    private final ClientRepository clients;
    private final UserRepository users;
    private final PropertyRepository properties;
    private final CalendarEventRepository events;
    private final ClientRequirementRepository requirements;

    public ClientService(ClientRepository clients, UserRepository users,
                         PropertyRepository properties, CalendarEventRepository events,
                         ClientRequirementRepository requirements) {
        this.clients = clients;
        this.users = users;
        this.properties = properties;
        this.events = events;
        this.requirements = requirements;
    }

    @Transactional
    public ClientResponse create(CreateClientRequest request, User author) {
        validate(request);

        User agent = resolveAgent(request.agentId(), author);

        Client client = new Client(
                author.getAgency(),
                agent,
                author,
                request.firstName().trim(),
                request.lastName().trim());

        client.setPhone(PhoneNumber.normalize(request.phone()));
        client.setEmail(normalizeEmail(request.email()));
        client.setSource(request.source());
        client.setStatus(orDefault(request.status(), ClientStatus.ACTIVE));
        client.setNotes(trimToNull(request.notes()));

        Client saved = clients.save(client);
        // Świeży klient nie ma jeszcze powierzonych ofert ani poszukiwań.
        return ClientResponse.from(saved, List.of(), List.of());
    }

    @Transactional
    public ClientResponse update(UUID id, CreateClientRequest request, User actor) {
        Client client = clients.findByIdAndAgencyId(id, actor.getAgency().getId())
                .orElseThrow(ClientNotFoundException::new);

        validate(request);

        client.setFirstName(request.firstName().trim());
        client.setLastName(request.lastName().trim());
        client.setPhone(PhoneNumber.normalize(request.phone()));
        client.setEmail(normalizeEmail(request.email()));
        client.setSource(request.source());
        client.setStatus(orDefault(request.status(), client.getStatus()));
        client.setNotes(trimToNull(request.notes()));

        // Pusty agentId przy edycji = nie zmieniaj opiekuna.
        if (request.agentId() != null) {
            client.setAgent(resolveAgent(request.agentId(), actor));
        }

        return card(clients.save(client));
    }

    @Transactional
    public void delete(UUID id, User actor) {
        Client client = clients.findByIdAndAgencyId(id, actor.getAgency().getId())
                .orElseThrow(ClientNotFoundException::new);

        // Powierzone oferty zostają w systemie, ale tracą właściciela. Bez tego
        // FK properties.owner_client_id zablokowałoby usunięcie klienta.
        for (Property property : properties.findByOwnerId(client.getId())) {
            property.setOwner(null);
        }

        // To samo z terminami: historia kontaktu zostaje, znika tylko powiązanie.
        // Bez tego klienta, z którym cokolwiek umówiono, nie dało się usunąć.
        events.detachClient(id);

        // Poszukiwania znikają razem z klientem (ON DELETE CASCADE w V13).
        // Bez osoby, która szuka, nie mają żadnej wartości.

        clients.delete(client);
    }

    @Transactional(readOnly = true)
    public Page<ClientSummary> list(User viewer, ClientStatus status, String search, Pageable pageable) {
        UUID agencyId = viewer.getAgency().getId();

        Page<Client> page;
        String term = trimToNull(search);
        if (term != null) {
            page = clients.search(agencyId, SearchTerms.like(term), SearchTerms.phoneLike(term), pageable);
        } else if (status != null) {
            page = clients.findByAgencyIdAndStatus(agencyId, status, pageable);
        } else {
            page = clients.findByAgencyId(agencyId, pageable);
        }

        Map<UUID, long[]> offers = offerCountsFor(page.getContent());
        Map<UUID, long[]> searches = requirementCountsFor(page.getContent());
        return page.map(client -> ClientSummary.from(
                client,
                offers.getOrDefault(client.getId(), EMPTY_COUNTS),
                searches.getOrDefault(client.getId(), EMPTY_COUNTS)));
    }

    @Transactional(readOnly = true)
    public ClientResponse get(UUID id, User viewer) {
        Client client = clients.findByIdAndAgencyId(id, viewer.getAgency().getId())
                .orElseThrow(ClientNotFoundException::new);
        return card(client);
    }

    /** Pełna karta: klient z ofertami i poszukiwaniami. */
    private ClientResponse card(Client client) {
        return ClientResponse.from(
                client,
                properties.findByOwnerId(client.getId()),
                requirements.findByClientIdAndAgencyId(client.getId(), client.getAgency().getId()));
    }

    /**
     * Przypisuje ofertę do klienta jako jej właściciela. Obie strony muszą być
     * z biura osoby wykonującej operację. Inaczej dałoby się skojarzyć cudzą
     * ofertę albo cudzego klienta.
     */
    @Transactional
    public ClientResponse assignProperty(UUID clientId, UUID propertyId, User actor) {
        UUID agencyId = actor.getAgency().getId();

        Client client = clients.findByIdAndAgencyId(clientId, agencyId)
                .orElseThrow(ClientNotFoundException::new);
        Property property = properties.findByIdAndAgencyId(propertyId, agencyId)
                .orElseThrow(PropertyNotFoundException::new);

        property.setOwner(client);
        properties.save(property);

        return card(client);
    }

    @Transactional
    public void unassignProperty(UUID clientId, UUID propertyId, User actor) {
        UUID agencyId = actor.getAgency().getId();

        Property property = properties.findByIdAndAgencyId(propertyId, agencyId)
                .orElseThrow(PropertyNotFoundException::new);

        // Odpięcie ma sens tylko wtedy, gdy oferta faktycznie należy do tego
        // klienta. Inaczej milcząco zmienialibyśmy powiązanie czyjejś oferty.
        if (property.getOwner() == null || !property.getOwner().getId().equals(clientId)) {
            throw new PropertyNotFoundException();
        }

        property.setOwner(null);
        properties.save(property);
    }

    /** Reguły dotyczące relacji między polami. Poza zasięgiem Bean Validation. */
    private void validate(CreateClientRequest request) {
        boolean hasPhone = trimToNull(request.phone()) != null;
        boolean hasEmail = trimToNull(request.email()) != null;
        if (!hasPhone && !hasEmail) {
            throw new BusinessValidationException(
                    Map.of("phone", "Podaj telefon lub e-mail. Inaczej nie będzie jak skontaktować się z klientem."));
        }
    }

    /** Opiekun kontaktu musi być z tego samego biura co osoba dodająca klienta. */
    private User resolveAgent(UUID agentId, User author) {
        if (agentId == null || agentId.equals(author.getId())) {
            return author;
        }

        return users.findById(agentId)
                .filter(candidate -> Objects.equals(
                        candidate.getAgency().getId(), author.getAgency().getId()))
                .orElseThrow(() -> new BusinessValidationException(
                        Map.of("agentId", "Wybrany opiekun nie należy do tego biura.")));
    }

    /** Oferty sell/rent per klient, jednym zapytaniem dla całej strony listy. */
    private Map<UUID, long[]> offerCountsFor(List<Client> page) {
        if (page.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = page.stream().map(Client::getId).toList();

        return properties.countByTransactionForOwners(ids).stream().collect(Collectors.toMap(
                OwnerTransactionCount::getOwnerId,
                row -> split(row.getTransactionType(), row.getCount()),
                ClientService::sum));
    }

    /** Aktywne poszukiwania kupna/najmu per klient. Tak samo jednym zapytaniem. */
    private Map<UUID, long[]> requirementCountsFor(List<Client> page) {
        if (page.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = page.stream().map(Client::getId).toList();

        return requirements.countByTransactionForClients(ids, RequirementStatus.ACTIVE).stream()
                .collect(Collectors.toMap(
                        RequirementTransactionCount::getClientId,
                        row -> split(row.getTransactionType(), row.getCount()),
                        ClientService::sum));
    }

    /** [sprzedaż, wynajem]. Z jednego wiersza grupowania. */
    private static long[] split(TransactionType type, long count) {
        long[] counts = new long[2];
        if (type == TransactionType.SALE) {
            counts[0] = count;
        } else if (type == TransactionType.RENT) {
            counts[1] = count;
        }
        return counts;
    }

    private static long[] sum(long[] a, long[] b) {
        return new long[]{a[0] + b[0], a[1] + b[1]};
    }

    private static final long[] EMPTY_COUNTS = {0L, 0L};

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

    private static String normalizeEmail(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
