package pl.delta.crm.matching;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.delta.crm.client.Client;
import pl.delta.crm.client.dictionary.RequirementStatus;
import pl.delta.crm.client.requirement.ClientRequirement;
import pl.delta.crm.client.requirement.ClientRequirementRepository;
import pl.delta.crm.client.requirement.dto.RequirementResponse;
import pl.delta.crm.error.PropertyNotFoundException;
import pl.delta.crm.error.RequirementNotFoundException;
import pl.delta.crm.matching.dto.ClientMatchResponse;
import pl.delta.crm.matching.dto.PropertyMatchResponse;
import pl.delta.crm.property.Property;
import pl.delta.crm.property.PropertyRepository;
import pl.delta.crm.property.dictionary.PropertyStatus;
import pl.delta.crm.property.dto.PropertySummary;
import pl.delta.crm.user.User;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Dopasowanie ofert do poszukiwań. W obie strony: „kto szuka czegoś takiego
 * jak ta oferta" i „co mamy dla tego klienta".
 *
 * <p>Liczone na żądanie, a nie zapisywane: kryteria i oferty zmieniają się
 * ciągle, a przechowywane dopasowania trzeba by unieważniać przy każdej edycji.
 * Zapytanie zawęża kandydatów po biurze, transakcji i rodzaju, więc w Javie
 * oceniamy już tylko garść wierszy.
 */
@Service
public class MatchingService {

    /**
     * Oferty, którymi da się jeszcze obsłużyć klienta. Robocza też. Agent
     * zakłada ofertę i od razu chce wiedzieć, do kogo z nią dzwonić, zanim
     * w ogóle trafi na portale.
     */
    static final Set<PropertyStatus> AVAILABLE =
            Set.of(PropertyStatus.DRAFT, PropertyStatus.ACTIVE, PropertyStatus.RESERVED);

    /** Najpierw pewne (bez ostrzeżeń), potem z większą liczbą mile widzianych cech. */
    private static final Comparator<Scored<?>> BEST_FIRST = Comparator
            .comparingLong((Scored<?> s) -> s.result().warnings())
            .thenComparing(s -> s.result().preferredHits(), Comparator.reverseOrder());

    private final PropertyRepository properties;
    private final ClientRequirementRepository requirements;

    public MatchingService(PropertyRepository properties, ClientRequirementRepository requirements) {
        this.properties = properties;
        this.requirements = requirements;
    }

    @Transactional(readOnly = true)
    public List<ClientMatchResponse> clientsForProperty(UUID propertyId, User viewer) {
        UUID agencyId = viewer.getAgency().getId();
        Property property = properties.findByIdAndAgencyId(propertyId, agencyId)
                .orElseThrow(PropertyNotFoundException::new);

        // Sprzedana czy zarchiwizowana oferta nikomu się już nie przyda.
        if (!AVAILABLE.contains(property.getStatus())) {
            return List.of();
        }

        return requirements.findMatchCandidates(agencyId, RequirementStatus.ACTIVE,
                        property.getTransactionType(), property.getPropertyType()).stream()
                // Właściciel nie szuka własnego mieszkania.
                .filter(r -> property.getOwner() == null
                        || !property.getOwner().getId().equals(r.getClient().getId()))
                .map(r -> new Scored<>(r, RequirementMatcher.evaluate(r, property)))
                .filter(s -> s.result().matches())
                .sorted(BEST_FIRST)
                .map(s -> {
                    Client client = s.subject().getClient();
                    return new ClientMatchResponse(
                            client.getId(),
                            client.fullName(),
                            client.getPhone(),
                            client.getEmail(),
                            client.getAgent().getFirstName() + " " + client.getAgent().getLastName(),
                            RequirementResponse.from(s.subject()),
                            s.result().criteria(),
                            s.result().warnings());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PropertyMatchResponse> propertiesForRequirement(UUID clientId, UUID requirementId, User viewer) {
        UUID agencyId = viewer.getAgency().getId();
        ClientRequirement requirement = requirements.findByIdAndClientIdAndAgencyId(requirementId, clientId, agencyId)
                .orElseThrow(RequirementNotFoundException::new);

        return propertiesFor(requirement, agencyId, clientId);
    }

    /**
     * Pasujące oferty dla niezapisanego poszukiwania (zgłoszenie z formularza
     * przed przyjęciem). Wołający odpowiada za to, że {@code agencyId} jest biurem
     * zalogowanego użytkownika.
     */
    @Transactional(readOnly = true)
    public List<PropertyMatchResponse> propertiesForDraft(ClientRequirement draft, UUID agencyId) {
        return propertiesFor(draft, agencyId, null);
    }

    private List<PropertyMatchResponse> propertiesFor(ClientRequirement requirement, UUID agencyId,
                                                      UUID seekerClientId) {
        return properties.findMatchCandidates(agencyId, requirement.getTransactionType(),
                        requirement.getPropertyTypes(), AVAILABLE).stream()
                .filter(p -> seekerClientId == null || p.getOwner() == null
                        || !p.getOwner().getId().equals(seekerClientId))
                .map(p -> new Scored<>(p, RequirementMatcher.evaluate(requirement, p)))
                .filter(s -> s.result().matches())
                .sorted(BEST_FIRST)
                .map(s -> new PropertyMatchResponse(
                        PropertySummary.withoutCover(s.subject()),
                        s.result().criteria(),
                        s.result().warnings()))
                .toList();
    }

    private record Scored<T>(T subject, MatchResult result) {
    }
}
