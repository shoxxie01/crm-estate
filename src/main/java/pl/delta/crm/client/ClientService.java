package pl.delta.crm.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.delta.crm.client.dictionary.ClientStatus;
import pl.delta.crm.client.dto.ClientResponse;
import pl.delta.crm.client.dto.ClientSummary;
import pl.delta.crm.client.dto.CreateClientRequest;
import pl.delta.crm.contact.PhoneNumber;
import pl.delta.crm.error.BusinessValidationException;
import pl.delta.crm.error.ClientNotFoundException;
import pl.delta.crm.error.PropertyNotFoundException;
import pl.delta.crm.property.OwnerTransactionCount;
import pl.delta.crm.property.Property;
import pl.delta.crm.property.PropertyRepository;
import pl.delta.crm.property.dictionary.TransactionType;
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

    public ClientService(ClientRepository clients, UserRepository users, PropertyRepository properties) {
        this.clients = clients;
        this.users = users;
        this.properties = properties;
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
        // Świeży klient nie ma jeszcze powierzonych ofert.
        return ClientResponse.from(saved, List.of());
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

        Client saved = clients.save(client);
        return ClientResponse.from(saved, properties.findByOwnerId(saved.getId()));
    }

    @Transactional
    public void delete(UUID id, User actor) {
        Client client = clients.findByIdAndAgencyId(id, actor.getAgency().getId())
                .orElseThrow(ClientNotFoundException::new);

        // Powierzone oferty zostają w systemie, ale tracą właściciela — bez tego
        // FK properties.owner_client_id zablokowałoby usunięcie klienta.
        for (Property property : properties.findByOwnerId(client.getId())) {
            property.setOwner(null);
        }

        clients.delete(client);
    }

    @Transactional(readOnly = true)
    public Page<ClientSummary> list(User viewer, ClientStatus status, String search, Pageable pageable) {
        UUID agencyId = viewer.getAgency().getId();

        Page<Client> page;
        String term = trimToNull(search);
        if (term != null) {
            page = clients.search(agencyId, "%" + term.toLowerCase(Locale.ROOT) + "%", pageable);
        } else if (status != null) {
            page = clients.findByAgencyIdAndStatus(agencyId, status, pageable);
        } else {
            page = clients.findByAgencyId(agencyId, pageable);
        }

        Map<UUID, long[]> intents = intentsFor(page.getContent());
        return page.map(client -> {
            long[] counts = intents.getOrDefault(client.getId(), EMPTY_COUNTS);
            return ClientSummary.from(client, counts[0], counts[1]);
        });
    }

    @Transactional(readOnly = true)
    public ClientResponse get(UUID id, User viewer) {
        Client client = clients.findByIdAndAgencyId(id, viewer.getAgency().getId())
                .orElseThrow(ClientNotFoundException::new);
        return ClientResponse.from(client, properties.findByOwnerId(client.getId()));
    }

    /**
     * Przypisuje ofertę do klienta jako jej właściciela. Obie strony muszą być
     * z biura osoby wykonującej operację — inaczej dałoby się skojarzyć cudzą
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

        return ClientResponse.from(client, properties.findByOwnerId(clientId));
    }

    @Transactional
    public void unassignProperty(UUID clientId, UUID propertyId, User actor) {
        UUID agencyId = actor.getAgency().getId();

        Property property = properties.findByIdAndAgencyId(propertyId, agencyId)
                .orElseThrow(PropertyNotFoundException::new);

        // Odpięcie ma sens tylko wtedy, gdy oferta faktycznie należy do tego
        // klienta — inaczej milcząco zmienialibyśmy powiązanie czyjejś oferty.
        if (property.getOwner() == null || !property.getOwner().getId().equals(clientId)) {
            throw new PropertyNotFoundException();
        }

        property.setOwner(null);
        properties.save(property);
    }

    /** Reguły dotyczące relacji między polami — poza zasięgiem Bean Validation. */
    private void validate(CreateClientRequest request) {
        boolean hasPhone = trimToNull(request.phone()) != null;
        boolean hasEmail = trimToNull(request.email()) != null;
        if (!hasPhone && !hasEmail) {
            throw new BusinessValidationException(
                    Map.of("phone", "Podaj telefon lub e-mail — inaczej nie będzie jak skontaktować się z klientem."));
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

    /** sell/rent per klient, jednym zapytaniem dla całej strony listy. */
    private Map<UUID, long[]> intentsFor(List<Client> page) {
        if (page.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = page.stream().map(Client::getId).toList();

        return properties.countByTransactionForOwners(ids).stream().collect(Collectors.toMap(
                OwnerTransactionCount::getOwnerId,
                row -> {
                    long[] counts = new long[2];
                    if (row.getTransactionType() == TransactionType.SALE) {
                        counts[0] = row.getCount();
                    } else if (row.getTransactionType() == TransactionType.RENT) {
                        counts[1] = row.getCount();
                    }
                    return counts;
                },
                (a, b) -> new long[]{a[0] + b[0], a[1] + b[1]}));
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
