package pl.delta.crm.client.dto;

import pl.delta.crm.client.Client;
import pl.delta.crm.client.dictionary.ClientStatus;
import pl.delta.crm.client.dictionary.LeadSource;
import pl.delta.crm.property.Property;
import pl.delta.crm.property.dictionary.TransactionType;
import pl.delta.crm.property.dto.PropertySummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Pełna karta klienta wraz z powierzonymi ofertami. Liczba ofert na sprzedaż
 * i na wynajem jest liczona z tej właśnie listy — to jedyne źródło rozróżnienia
 * sprzedający/wynajmujący.
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
        List<PropertySummary> ownedProperties,
        Instant createdAt,
        Instant updatedAt
) {

    public static ClientResponse from(Client client, List<Property> ownedProperties) {
        long sell = ownedProperties.stream()
                .filter(p -> p.getTransactionType() == TransactionType.SALE)
                .count();
        long rent = ownedProperties.stream()
                .filter(p -> p.getTransactionType() == TransactionType.RENT)
                .count();

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
                ownedProperties.stream().map(PropertySummary::withoutCover).toList(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }
}
