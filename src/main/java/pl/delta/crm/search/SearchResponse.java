package pl.delta.crm.search;

import pl.delta.crm.client.Client;
import pl.delta.crm.client.dictionary.ClientStatus;
import pl.delta.crm.property.Property;
import pl.delta.crm.property.dictionary.Currency;
import pl.delta.crm.property.dictionary.PropertyStatus;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.TransactionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Wyniki szybkiego wyszukiwania. Po kilka trafień z każdej grupy, tylko pola
 * potrzebne do jednego wiersza podpowiedzi. {@code clientsTotal} mówi, czy
 * warto pokazać link do pełnej listy klientów.
 */
public record SearchResponse(
        List<ClientHit> clients,
        long clientsTotal,
        List<PropertyHit> properties,
        long propertiesTotal
) {

    public static SearchResponse empty() {
        return new SearchResponse(List.of(), 0, List.of(), 0);
    }

    public record ClientHit(
            UUID id,
            String firstName,
            String lastName,
            String phone,
            String email,
            ClientStatus status
    ) {
        static ClientHit from(Client client) {
            return new ClientHit(
                    client.getId(),
                    client.getFirstName(),
                    client.getLastName(),
                    client.getPhone(),
                    client.getEmail(),
                    client.getStatus());
        }
    }

    public record PropertyHit(
            UUID id,
            String referenceNumber,
            String title,
            PropertyType propertyType,
            TransactionType transactionType,
            PropertyStatus status,
            String city,
            String district,
            String street,
            BigDecimal price,
            Currency priceCurrency
    ) {
        static PropertyHit from(Property property) {
            return new PropertyHit(
                    property.getId(),
                    property.getReferenceNumber(),
                    property.getTitle(),
                    property.getPropertyType(),
                    property.getTransactionType(),
                    property.getStatus(),
                    property.getAddress().getCity(),
                    property.getAddress().getDistrict(),
                    property.getAddress().getStreet(),
                    property.getPricing().getPrice(),
                    property.getPricing().getPriceCurrency());
        }
    }
}
