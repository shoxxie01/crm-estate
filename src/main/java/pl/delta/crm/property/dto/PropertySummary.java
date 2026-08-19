package pl.delta.crm.property.dto;

import pl.delta.crm.property.Property;
import pl.delta.crm.property.dictionary.Currency;
import pl.delta.crm.property.dictionary.MarketType;
import pl.delta.crm.property.dictionary.PropertyStatus;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Wiersz listy ofert. Tylko to, co widać w tabeli — bez ładowania kolekcji. */
public record PropertySummary(
        UUID id,
        String referenceNumber,
        String title,
        PropertyType propertyType,
        TransactionType transactionType,
        MarketType marketType,
        PropertyStatus status,
        BigDecimal price,
        Currency priceCurrency,
        BigDecimal pricePerSquareMeter,
        BigDecimal totalArea,
        Short roomsCount,
        String city,
        String district,
        String agentName,
        boolean readyForExport,
        Instant createdAt
) {

    public static PropertySummary from(Property property) {
        return new PropertySummary(
                property.getId(),
                property.getReferenceNumber(),
                property.getTitle(),
                property.getPropertyType(),
                property.getTransactionType(),
                property.getMarketType(),
                property.getStatus(),
                property.getPricing().getPrice(),
                property.getPricing().getPriceCurrency(),
                property.getPricing().getPricePerM2(),
                property.getTotalArea(),
                property.getRoomsCount(),
                property.getAddress().getCity(),
                property.getAddress().getDistrict(),
                property.getAgent().getFirstName() + " " + property.getAgent().getLastName(),
                property.readyForExport(),
                property.getCreatedAt()
        );
    }
}
