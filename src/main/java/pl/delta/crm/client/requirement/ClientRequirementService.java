package pl.delta.crm.client.requirement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.delta.crm.agency.Agency;
import pl.delta.crm.client.Client;
import pl.delta.crm.client.ClientRepository;
import pl.delta.crm.client.dictionary.RequirementStatus;
import pl.delta.crm.client.requirement.dto.RequirementRequest;
import pl.delta.crm.client.requirement.dto.RequirementResponse;
import pl.delta.crm.error.BusinessValidationException;
import pl.delta.crm.error.ClientNotFoundException;
import pl.delta.crm.error.RequirementNotFoundException;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.TransactionType;
import pl.delta.crm.user.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ClientRequirementService {

    /** Na karcie klienta najpierw to, nad czym agent pracuje, potem historia. */
    public static final Comparator<ClientRequirement> CARD_ORDER = Comparator
            .comparing((ClientRequirement r) -> r.getStatus() != RequirementStatus.ACTIVE)
            .thenComparing(ClientRequirement::getCreatedAt, Comparator.reverseOrder());

    private final ClientRequirementRepository requirements;
    private final ClientRepository clients;

    public ClientRequirementService(ClientRequirementRepository requirements, ClientRepository clients) {
        this.requirements = requirements;
        this.clients = clients;
    }

    @Transactional(readOnly = true)
    public List<RequirementResponse> list(UUID clientId, User viewer) {
        UUID agencyId = viewer.getAgency().getId();
        clients.findByIdAndAgencyId(clientId, agencyId).orElseThrow(ClientNotFoundException::new);

        return requirements.findByClientIdAndAgencyId(clientId, agencyId).stream()
                .sorted(CARD_ORDER)
                .map(RequirementResponse::from)
                .toList();
    }

    @Transactional
    public RequirementResponse create(UUID clientId, RequirementRequest request, User author) {
        Client client = clients.findByIdAndAgencyId(clientId, author.getAgency().getId())
                .orElseThrow(ClientNotFoundException::new);

        validate(request);

        ClientRequirement requirement = new ClientRequirement(client, author, request.transactionType());
        if (request.status() != null) {
            requirement.setStatus(request.status());
        }
        apply(request, requirement);

        return RequirementResponse.from(requirements.save(requirement));
    }

    @Transactional
    public RequirementResponse update(UUID clientId, UUID id, RequirementRequest request, User actor) {
        ClientRequirement requirement = find(clientId, id, actor);

        validate(request);

        requirement.setTransactionType(request.transactionType());
        if (request.status() != null) {
            requirement.setStatus(request.status());
        }
        apply(request, requirement);

        return RequirementResponse.from(requirements.save(requirement));
    }

    @Transactional
    public RequirementResponse changeStatus(UUID clientId, UUID id, RequirementStatus status, User actor) {
        ClientRequirement requirement = find(clientId, id, actor);
        requirement.setStatus(status);
        return RequirementResponse.from(requirements.save(requirement));
    }

    @Transactional
    public void delete(UUID clientId, UUID id, User actor) {
        requirements.delete(find(clientId, id, actor));
    }

    private ClientRequirement find(UUID clientId, UUID id, User actor) {
        return requirements.findByIdAndClientIdAndAgencyId(id, clientId, actor.getAgency().getId())
                .orElseThrow(RequirementNotFoundException::new);
    }

    /**
     * Niezapisane poszukiwanie z kryteriów formularza. Do podglądu dopasowań.
     * Te same reguły co przy zapisie, żeby podgląd nie obiecywał czegoś, czego
     * przyjęte poszukiwanie potem nie znajdzie.
     */
    public static ClientRequirement draft(Agency agency, RequirementRequest request) {
        ClientRequirement draft = ClientRequirement.draft(agency, request.transactionType());
        apply(request, draft);
        return draft;
    }

    private static void apply(RequirementRequest request, ClientRequirement requirement) {
        boolean purchase = request.transactionType() == TransactionType.SALE;

        requirement.setPropertyTypes(request.propertyTypes());
        requirement.setMarketType(request.marketType());
        requirement.setLocations(locations(request.locations()));
        requirement.setPriceMin(request.priceMin());
        requirement.setPriceMax(request.priceMax());
        requirement.setAreaMin(request.areaMin());
        requirement.setAreaMax(request.areaMax());
        requirement.setRoomsMin(request.roomsMin());
        requirement.setRoomsMax(request.roomsMax());
        requirement.setFloorMin(request.floorMin());
        requirement.setFloorMax(request.floorMax());
        requirement.setExcludeTopFloor(Boolean.TRUE.equals(request.excludeTopFloor()));
        // Formularz chowa finansowanie przy najmie. Gdyby jednak przyszło (np. po
        // przełączeniu z kupna), czyścimy je zamiast odrzucać cały zapis.
        requirement.setFinancing(purchase ? request.financing() : null);
        requirement.setMoveInDate(request.moveInDate());
        requirement.setFeatures(orEmpty(request.requiredFeatures()), orEmpty(request.preferredFeatures()));
        requirement.setNotes(trimToNull(request.notes()));
    }

    /**
     * Relacje między polami. Poza zasięgiem Bean Validation. Publiczne, bo te
     * same reguły obowiązują kryteria wysłane z formularza zgłoszeniowego.
     */
    public static void validate(RequirementRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (request.transactionType() == TransactionType.SALE
                && request.propertyTypes().stream().anyMatch(PropertyType::rentOnly)) {
            errors.put("propertyTypes", "Pokój można tylko najmować. Usuń go albo zmień transakcję na najem.");
        }

        checkRange(errors, "priceMax", request.priceMin(), request.priceMax(),
                "Budżet „do” nie może być niższy niż „od”.");
        checkRange(errors, "areaMax", request.areaMin(), request.areaMax(),
                "Metraż „do” nie może być mniejszy niż „od”.");
        checkRange(errors, "roomsMax", request.roomsMin(), request.roomsMax(),
                "Liczba pokoi „do” nie może być mniejsza niż „od”.");
        checkRange(errors, "floorMax", request.floorMin(), request.floorMax(),
                "Piętro „do” nie może być niższe niż „od”.");

        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }
    }

    private static <T extends Comparable<T>> void checkRange(
            Map<String, String> errors, String field, T min, T max, String message) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            errors.put(field, message);
        }
    }

    /** Przycięte, bez pustych dzielnic i bez powtórzeń (wielkość liter bez znaczenia). */
    private static List<RequirementLocation> locations(List<RequirementRequest.LocationRequest> requested) {
        if (requested == null) {
            return List.of();
        }

        Set<String> seen = new HashSet<>();
        List<RequirementLocation> result = new ArrayList<>();
        for (RequirementRequest.LocationRequest location : requested) {
            String city = location.city().trim();
            String district = trimToNull(location.district());
            String key = (city + "|" + (district == null ? "" : district)).toLowerCase(Locale.ROOT);
            if (seen.add(key)) {
                result.add(new RequirementLocation(city, district));
            }
        }
        return result;
    }

    private static <T> Set<T> orEmpty(Set<T> value) {
        return value == null ? Set.of() : value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
