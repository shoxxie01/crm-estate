package pl.delta.crm.client.requirement.dto;

import pl.delta.crm.client.dictionary.Financing;
import pl.delta.crm.client.dictionary.RequirementStatus;
import pl.delta.crm.client.requirement.ClientRequirement;
import pl.delta.crm.property.dictionary.Feature;
import pl.delta.crm.property.dictionary.MarketType;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Poszukiwanie na karcie klienta. Rodzaje i cechy w kolejności słownika, nie zapisu. */
public record RequirementResponse(
        UUID id,
        UUID clientId,
        RequirementStatus status,
        TransactionType transactionType,
        List<PropertyType> propertyTypes,
        MarketType marketType,
        List<LocationView> locations,
        BigDecimal priceMin,
        BigDecimal priceMax,
        BigDecimal areaMin,
        BigDecimal areaMax,
        Short roomsMin,
        Short roomsMax,
        Short floorMin,
        Short floorMax,
        boolean excludeTopFloor,
        Financing financing,
        LocalDate moveInDate,
        List<Feature> requiredFeatures,
        List<Feature> preferredFeatures,
        String notes,
        String createdByName,
        Instant createdAt,
        Instant updatedAt
) {

    public record LocationView(String city, String district) {
    }

    public static RequirementResponse from(ClientRequirement requirement) {
        Map<Feature, Boolean> features = requirement.getFeatures();

        return new RequirementResponse(
                requirement.getId(),
                requirement.getClient().getId(),
                requirement.getStatus(),
                requirement.getTransactionType(),
                requirement.getPropertyTypes().stream().sorted().toList(),
                requirement.getMarketType(),
                requirement.getLocations().stream()
                        .map(location -> new LocationView(location.getCity(), location.getDistrict()))
                        .toList(),
                requirement.getPriceMin(),
                requirement.getPriceMax(),
                requirement.getAreaMin(),
                requirement.getAreaMax(),
                requirement.getRoomsMin(),
                requirement.getRoomsMax(),
                requirement.getFloorMin(),
                requirement.getFloorMax(),
                requirement.isExcludeTopFloor(),
                requirement.getFinancing(),
                requirement.getMoveInDate(),
                features.entrySet().stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey)
                        .sorted()
                        .toList(),
                features.entrySet().stream()
                        .filter(entry -> !entry.getValue())
                        .map(Map.Entry::getKey)
                        .sorted()
                        .toList(),
                requirement.getNotes(),
                requirement.getCreatedBy().getFirstName() + " " + requirement.getCreatedBy().getLastName(),
                requirement.getCreatedAt(),
                requirement.getUpdatedAt()
        );
    }
}
