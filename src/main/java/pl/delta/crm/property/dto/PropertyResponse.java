package pl.delta.crm.property.dto;

import pl.delta.crm.property.Address;
import pl.delta.crm.property.BuildingDetails;
import pl.delta.crm.property.CommercialDetails;
import pl.delta.crm.property.EnergyCertificate;
import pl.delta.crm.property.LandDetails;
import pl.delta.crm.property.PortalPublication;
import pl.delta.crm.property.Pricing;
import pl.delta.crm.property.Property;
import pl.delta.crm.property.PropertyMedia;
import pl.delta.crm.property.dictionary.CommercialUse;
import pl.delta.crm.property.dictionary.Currency;
import pl.delta.crm.property.dictionary.Feature;
import pl.delta.crm.property.dictionary.HeatingType;
import pl.delta.crm.property.dictionary.MarketType;
import pl.delta.crm.property.dictionary.MediaType;
import pl.delta.crm.property.dictionary.Portal;
import pl.delta.crm.property.dictionary.PropertyStatus;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.PublicationStatus;
import pl.delta.crm.property.dictionary.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Pełny widok oferty. */
public record PropertyResponse(
        UUID id,
        String referenceNumber,
        PropertyType propertyType,
        TransactionType transactionType,
        MarketType marketType,
        PropertyStatus status,
        String title,
        String description,
        PricingView pricing,
        AreaView area,
        AddressView address,
        BuildingView building,
        EnergyView energy,
        LandView land,
        CommercialView commercial,
        String garageType,
        Short occupants,
        String roomBathroom,
        LocalDate availableFrom,
        Set<Feature> features,
        Set<HeatingType> heatingTypes,
        Set<CommercialUse> commercialUses,
        List<MediaView> media,
        List<PublicationView> publications,
        String videoUrl,
        String panoramaUrl,
        String privateNotes,
        String keysInfo,
        boolean exportable,
        boolean readyForExport,
        AgentView agent,
        Instant createdAt,
        Instant updatedAt
) {

    public record PricingView(BigDecimal price, Currency priceCurrency, boolean priceNegotiable,
                              BigDecimal pricePerSquareMeter, BigDecimal rent, Currency rentCurrency,
                              boolean priceIncludesRent, BigDecimal deposit, Currency depositCurrency,
                              BigDecimal commissionPercent) {
    }

    public record AreaView(BigDecimal totalArea, BigDecimal usableArea, BigDecimal plotArea,
                           Short roomsCount, Short bathroomsCount, Short floorNo,
                           Short buildingFloorsCount, BigDecimal ceilingHeight) {
    }

    public record AddressView(String countryCode, String voivodeship, String county, String commune,
                              String city, String district, String street, String buildingNumber,
                              String apartmentNumber, String postalCode, BigDecimal latitude,
                              BigDecimal longitude, boolean hideExactAddress,
                              String terytSimc, String terytUlic) {
    }

    public record BuildingView(Short buildYear, String buildingType, String buildingMaterial,
                               String constructionStatus, String windowsType, String roofType,
                               String roofing, String garretType, String ownershipForm,
                               String surroundings, Boolean furnished) {
    }

    public record EnergyView(BigDecimal energyPrimary, BigDecimal energyFinal, String energyClass,
                             String certificateNumber, LocalDate issuedAt, LocalDate validUntil,
                             boolean exempt, String exemptNote, boolean satisfiesLegalRequirement) {
    }

    public record LandView(String plotType, String dimensions, String roadAccess,
                           Boolean fenced, String zoningPlan) {
    }

    public record CommercialView(String structure, String flooring, String parkingType,
                                 Boolean officeSpace, Boolean socialFacilities, Boolean loadingRamp,
                                 BigDecimal powerConnectionKw, BigDecimal floorLoadPerM2,
                                 Short loadingDocksCount) {
    }

    public record MediaView(UUID id, MediaType mediaType, String fileName, String storageKey,
                            short position, String caption, Integer widthPx, Integer heightPx,
                            long sizeBytes, boolean meetsPortalRequirements) {
    }

    public record PublicationView(Portal portal, PublicationStatus status, String externalId,
                                  String externalUrl, Instant lastExportedAt, String lastError) {
    }

    public record AgentView(UUID id, String name, String email) {
    }

    public static PropertyResponse from(Property p) {
        Pricing pricing = p.getPricing();
        Address address = p.getAddress();
        BuildingDetails building = p.getBuilding();
        EnergyCertificate energy = p.getEnergyCertificate();
        LandDetails land = p.getLand();
        CommercialDetails commercial = p.getCommercial();

        return new PropertyResponse(
                p.getId(),
                p.getReferenceNumber(),
                p.getPropertyType(),
                p.getTransactionType(),
                p.getMarketType(),
                p.getStatus(),
                p.getTitle(),
                p.getDescription(),
                new PricingView(
                        pricing.getPrice(), pricing.getPriceCurrency(), pricing.isPriceNegotiable(),
                        pricing.getPricePerM2(), pricing.getRent(),
                        pricing.getRentCurrency(), pricing.isPriceIncludesRent(), pricing.getDeposit(),
                        pricing.getDepositCurrency(), pricing.getCommissionPercent()),
                new AreaView(
                        p.getTotalArea(), p.getUsableArea(), p.getPlotArea(), p.getRoomsCount(),
                        p.getBathroomsCount(), p.getFloorNo(), p.getBuildingFloorsCount(),
                        p.getCeilingHeight()),
                new AddressView(
                        address.getCountryCode(), name(address.getVoivodeship()), address.getCounty(),
                        address.getCommune(), address.getCity(), address.getDistrict(),
                        address.getStreet(), address.getBuildingNumber(), address.getApartmentNumber(),
                        address.getPostalCode(), address.getLatitude(), address.getLongitude(),
                        address.isHideExactAddress(), address.getTerytSimc(), address.getTerytUlic()),
                new BuildingView(
                        building.getBuildYear(), name(building.getBuildingType()),
                        name(building.getBuildingMaterial()), name(building.getConstructionStatus()),
                        name(building.getWindowsType()), name(building.getRoofType()),
                        name(building.getRoofing()), name(building.getGarretType()),
                        name(building.getOwnershipForm()), name(building.getSurroundings()),
                        building.getFurnished()),
                new EnergyView(
                        energy.getEnergyPrimary(), energy.getEnergyFinal(), name(energy.getEnergyClass()),
                        energy.getCertificateNumber(), energy.getIssuedAt(), energy.getValidUntil(),
                        energy.isExempt(), energy.getExemptNote(), energy.satisfiesLegalRequirement()),
                new LandView(
                        name(land.getPlotType()), land.getDimensions(), name(land.getRoadAccess()),
                        land.getFenced(), land.getZoningPlan()),
                new CommercialView(
                        name(commercial.getStructure()), name(commercial.getFlooring()),
                        name(commercial.getParkingType()), commercial.getOfficeSpace(),
                        commercial.getSocialFacilities(), commercial.getLoadingRamp(),
                        commercial.getPowerConnectionKw(), commercial.getFloorLoadPerM2(),
                        commercial.getLoadingDocksCount()),
                name(p.getGarageType()),
                p.getOccupants(),
                name(p.getRoomBathroom()),
                p.getAvailableFrom(),
                // Kopie, nie referencje. Kolekcje encji są leniwe, a rekord
                // przeżywa transakcję — Jackson rozwijałby je, kiedy sesja
                // jest już zamknięta. Set.copyOf iteruje je tu i teraz.
                Set.copyOf(p.getFeatures()),
                Set.copyOf(p.getHeatingTypes()),
                Set.copyOf(p.getCommercialUses()),
                p.getMedia().stream().map(PropertyResponse::mediaView).toList(),
                p.getPublications().stream().map(PropertyResponse::publicationView).toList(),
                p.getVideoUrl(),
                p.getPanoramaUrl(),
                p.getPrivateNotes(),
                p.getKeysInfo(),
                p.isExportable(),
                p.readyForExport(),
                new AgentView(p.getAgent().getId(),
                        p.getAgent().getFirstName() + " " + p.getAgent().getLastName(),
                        p.getAgent().getEmail()),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private static MediaView mediaView(PropertyMedia m) {
        return new MediaView(m.getId(), m.getMediaType(), m.getFileName(), m.getStorageKey(),
                m.getPosition(), m.getCaption(), m.getWidthPx(), m.getHeightPx(),
                m.getSizeBytes(), m.meetsPortalRequirements());
    }

    private static PublicationView publicationView(PortalPublication pub) {
        return new PublicationView(pub.getPortal(), pub.getStatus(), pub.getExternalId(),
                pub.getExternalUrl(), pub.getLastExportedAt(), pub.getLastError());
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
