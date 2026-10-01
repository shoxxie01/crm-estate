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

/**
 * Wiersz listy ofert. Tylko to, co widać w tabeli. Bez ładowania kolekcji.
 *
 * <p>Miniatura zdjęcia głównego przychodzi z zewnątrz, a nie z {@code property
 * .getMedia()}: sięgnięcie po kolekcję rozwinęłoby ją dla każdego wiersza z
 * osobna. Serwis pobiera zdjęcia z pozycji 0 dla całej strony jednym zapytaniem.
 */
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
        /** Miniatura zdjęcia głównego; null, gdy oferta nie ma jeszcze zdjęć. */
        String coverThumbnailUrl,
        Instant createdAt
) {

    /**
     * Wariant dla list, które pokazują oferty tekstowo. Jak sekcja „Powierzone
     * oferty" na karcie klienta. Nazwany wprost, żeby brak miniatury był decyzją
     * widoczną w miejscu wywołania, a nie przeoczonym {@code null}-em.
     */
    public static PropertySummary withoutCover(Property property) {
        return from(property, null);
    }

    public static PropertySummary from(Property property, String coverThumbnailUrl) {
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
                coverThumbnailUrl,
                property.getCreatedAt()
        );
    }
}
