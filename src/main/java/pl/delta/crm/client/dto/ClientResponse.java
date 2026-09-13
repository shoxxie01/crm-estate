package pl.delta.crm.client.dto;

import pl.delta.crm.client.Client;
import pl.delta.crm.client.dictionary.ClientStatus;
import pl.delta.crm.client.dictionary.LeadSource;
import pl.delta.crm.client.dictionary.RequirementStatus;
import pl.delta.crm.client.requirement.ClientRequirement;
import pl.delta.crm.client.requirement.ClientRequirementService;
import pl.delta.crm.client.requirement.dto.RequirementResponse;
import pl.delta.crm.property.Property;
import pl.delta.crm.property.dictionary.TransactionType;
import pl.delta.crm.property.dto.PropertySummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Pełna karta klienta wraz z powierzonymi ofertami i poszukiwaniami. Role są
 * liczone z tych właśnie list: sprzedający / wynajmujący z ofert, kupujący /
 * najemca z aktywnych poszukiwań.
 */
public record ClientResponse(
        UUID id,
        String firstName,
        String lastName,
        String phone,
        String email,
        LeadSource source,
        ClientStatus status,
        String notes,
        UUID agentId,
        String agentName,
        long sellCount,
        long rentCount,
        long buyerCount,
        long tenantCount,
        List<PropertySummary> ownedProperties,
        List<RequirementResponse> requirements,
        Instant createdAt,
        Instant updatedAt
) {

    public static ClientResponse from(Client client, List<Property> ownedProperties,
                                      List<ClientRequirement> requirements) {
        long sell = ownedProperties.stream()
                .filter(p -> p.getTransactionType() == TransactionType.SALE)
                .count();
        long rent = ownedProperties.stream()
                .filter(p -> p.getTransactionType() == TransactionType.RENT)
                .count();
        long buyer = activeCount(requirements, TransactionType.SALE);
        long tenant = activeCount(requirements, TransactionType.RENT);

        return new ClientResponse(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getPhone(),
                client.getEmail(),
                client.getSource(),
                client.getStatus(),
                client.getNotes(),
                client.getAgent().getId(),
                client.getAgent().getFirstName() + " " + client.getAgent().getLastName(),
                sell,
                rent,
                buyer,
                tenant,
                ownedProperties.stream().map(PropertySummary::withoutCover).toList(),
                requirements.stream()
                        .sorted(ClientRequirementService.CARD_ORDER)
                        .map(RequirementResponse::from)
                        .toList(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }

    private static long activeCount(List<ClientRequirement> requirements, TransactionType type) {
        return requirements.stream()
                .filter(r -> r.getStatus() == RequirementStatus.ACTIVE && r.getTransactionType() == type)
                .count();
    }
}
