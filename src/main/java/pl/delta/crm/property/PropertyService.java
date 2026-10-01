package pl.delta.crm.property;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.delta.crm.calendar.CalendarEventRepository;
import pl.delta.crm.error.BusinessValidationException;
import pl.delta.crm.error.PropertyNotFoundException;
import pl.delta.crm.property.dictionary.Currency;
import pl.delta.crm.property.dictionary.PropertyStatus;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.TransactionType;
import pl.delta.crm.property.dto.AddressRequest;
import pl.delta.crm.property.dto.AreaRequest;
import pl.delta.crm.property.dto.BuildingRequest;
import pl.delta.crm.property.dto.CommercialRequest;
import pl.delta.crm.property.dto.CreatePropertyRequest;
import pl.delta.crm.property.dto.EnergyRequest;
import pl.delta.crm.property.dto.LandRequest;
import pl.delta.crm.property.dto.PropertyResponse;
import pl.delta.crm.property.dto.PropertySummary;
import pl.delta.crm.storage.MediaStorage;
import pl.delta.crm.user.User;
import pl.delta.crm.user.UserRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class PropertyService {

    private final PropertyRepository properties;
    private final PropertyMediaRepository media;
    private final PropertyMediaService mediaService;
    private final CalendarEventRepository events;
    private final UserRepository users;
    private final MediaStorage storage;

    public PropertyService(PropertyRepository properties, PropertyMediaRepository media,
                           PropertyMediaService mediaService, CalendarEventRepository events,
                           UserRepository users, MediaStorage storage) {
        this.properties = properties;
        this.media = media;
        this.mediaService = mediaService;
        this.events = events;
        this.users = users;
        this.storage = storage;
    }

    @Transactional
    public PropertyResponse create(CreatePropertyRequest request, User author) {
        UUID agencyId = author.getAgency().getId();

        validate(request);

        String referenceNumber = nextReferenceNumber(agencyId);
        User agent = resolveAgent(request.agentId(), author);

        Pricing pricing = new Pricing(
                request.pricing().price(),
                orDefault(request.pricing().priceCurrency(), Currency.PLN));
        applyPricing(pricing, request.pricing(), request.area().totalArea());

        Address address = buildAddress(request.address());

        Property property = new Property(
                author.getAgency(),
                referenceNumber,
                agent,
                author,
                request.propertyType(),
                request.transactionType(),
                request.marketType(),
                request.title().trim(),
                request.description().trim(),
                pricing,
                request.area().totalArea(),
                address);

        applyArea(property, request.area());
        applyBuilding(property.getBuilding(), request.building());
        applyEnergy(property.getEnergyCertificate(), request.energy());
        applyLand(property.getLand(), request.land());
        applyCommercial(property.getCommercial(), request.commercial());
        property.setGarageType(request.garageType());
        property.setOccupants(request.occupants());
        property.setRoomBathroom(request.roomBathroom());

        property.setStatus(orDefault(request.status(), PropertyStatus.DRAFT));
        property.setAvailableFrom(request.availableFrom());
        property.setVideoUrl(request.videoUrl());
        property.setPanoramaUrl(request.panoramaUrl());
        property.setPrivateNotes(request.privateNotes());
        property.setKeysInfo(request.keysInfo());
        property.setExportable(orDefault(request.exportable(), Boolean.TRUE));

        if (request.features() != null) {
            property.setFeatures(request.features());
        }
        if (request.heatingTypes() != null) {
            property.setHeatingTypes(request.heatingTypes());
        }
        if (request.commercialUses() != null) {
            property.setCommercialUses(request.commercialUses());
        }

        return PropertyResponse.from(properties.save(property), storage::url);
    }

    /**
     * Pełne nadpisanie oferty. Numer oferty, autor, właściciel, zdjęcia
     * i publikacje zostają nietknięte. Zmienia się tylko to, co obejmuje
     * formularz. Pola spoza formularza (np. typ dachu) wracają do wartości
     * domyślnej, dokładnie jak przy tworzeniu. Formularz jest granicą tego,
     * co edytowalne.
     */
    @Transactional
    public PropertyResponse update(UUID id, CreatePropertyRequest request, User editor) {
        Property property = properties.findByIdAndAgencyId(id, editor.getAgency().getId())
                .orElseThrow(PropertyNotFoundException::new);

        validate(request);

        property.setPropertyType(request.propertyType());
        property.setTransactionType(request.transactionType());
        property.setMarketType(request.marketType());
        property.setTitle(request.title().trim());
        property.setDescription(request.description().trim());
        property.setTotalArea(request.area().totalArea());

        Pricing pricing = new Pricing(
                request.pricing().price(),
                orDefault(request.pricing().priceCurrency(), Currency.PLN));
        applyPricing(pricing, request.pricing(), request.area().totalArea());
        property.setPricing(pricing);

        property.setAddress(buildAddress(request.address()));

        applyArea(property, request.area());
        applyBuilding(property.getBuilding(), request.building());
        applyEnergy(property.getEnergyCertificate(), request.energy());
        applyLand(property.getLand(), request.land());
        applyCommercial(property.getCommercial(), request.commercial());
        property.setGarageType(request.garageType());
        property.setOccupants(request.occupants());
        property.setRoomBathroom(request.roomBathroom());

        property.setStatus(orDefault(request.status(), property.getStatus()));
        property.setAvailableFrom(request.availableFrom());
        property.setVideoUrl(request.videoUrl());
        property.setPanoramaUrl(request.panoramaUrl());
        property.setPrivateNotes(request.privateNotes());
        property.setKeysInfo(request.keysInfo());
        property.setExportable(orDefault(request.exportable(), Boolean.TRUE));

        // Pusty agentId przy edycji znaczy „nie ruszaj prowadzącego". Inaczej
        // każda zmiana przez inną osobę przepisywałaby ofertę na nią.
        if (request.agentId() != null) {
            property.setAgent(resolveAgent(request.agentId(), editor));
        }
        if (request.features() != null) {
            property.setFeatures(request.features());
        }
        if (request.heatingTypes() != null) {
            property.setHeatingTypes(request.heatingTypes());
        }
        if (request.commercialUses() != null) {
            property.setCommercialUses(request.commercialUses());
        }

        return PropertyResponse.from(properties.save(property), storage::url);
    }

    @Transactional
    public void delete(UUID id, User actor) {
        Property property = properties.findByIdAndAgencyId(id, actor.getAgency().getId())
                .orElseThrow(PropertyNotFoundException::new);
        // Terminy nie znikają razem z ofertą. Tracą tylko powiązanie. Historia
        // pokazów zostaje, a bez tego oferta z choćby jednym terminem w ogóle
        // nie dałaby się usunąć (klucz obcy w calendar_events bez ON DELETE).
        events.detachProperty(id);

        // Zdjęcia, publikacje i kolekcje cech znikają kaskadowo (cascade/orphan
        // po stronie JPA oraz ON DELETE CASCADE w migracji V3). Kaskada obejmuje
        // jednak wyłącznie bazę. Pliki trzeba skasować osobno, po commicie.
        mediaService.purgeStorageFor(property);
        properties.delete(property);
    }

    @Transactional(readOnly = true)
    public Page<PropertySummary> list(User viewer, PropertyStatus status, PropertyType type,
                                      TransactionType transaction, Pageable pageable) {
        UUID agencyId = viewer.getAgency().getId();

        Page<Property> page;
        if (status != null) {
            page = properties.findByAgencyIdAndStatus(agencyId, status, pageable);
        } else if (type != null) {
            page = properties.findByAgencyIdAndPropertyType(agencyId, type, pageable);
        } else if (transaction != null) {
            page = properties.findByAgencyIdAndTransactionType(agencyId, transaction, pageable);
        } else {
            page = properties.findByAgencyId(agencyId, pageable);
        }

        Map<UUID, String> covers = coverThumbnailsFor(page.getContent());
        return page.map(property -> PropertySummary.from(property, covers.get(property.getId())));
    }

    @Transactional(readOnly = true)
    public PropertyResponse get(UUID id, User viewer) {
        return properties.findByIdAndAgencyId(id, viewer.getAgency().getId())
                .map(property -> PropertyResponse.from(property, storage::url))
                .orElseThrow(PropertyNotFoundException::new);
    }

    /**
     * Miniatury zdjęć głównych dla całej strony listy. Jednym zapytaniem po
     * pozycji 0, zamiast rozwijania kolekcji zdjęć w każdym wierszu z osobna.
     */
    private Map<UUID, String> coverThumbnailsFor(List<Property> page) {
        if (page.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = page.stream().map(Property::getId).toList();

        Map<UUID, String> covers = new LinkedHashMap<>();
        for (PropertyMedia cover : media.findByPropertyIdInAndPosition(ids, (short) 0)) {
            covers.put(cover.getProperty().getId(), storage.url(cover.thumbnailKey()));
        }
        return covers;
    }

    /**
     * Reguły, których nie da się wyrazić adnotacją na pojedynczym polu, bo
     * dotyczą zależności między nimi.
     */
    private void validate(CreatePropertyRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        // Otodom odrzuca mieszkanie i dom bez liczby pokoi (RoomsNum).
        if (request.propertyType().requiresRoomsCount() && request.area().roomsCount() == null) {
            errors.put("area.roomsCount",
                    "Liczba pokoi jest wymagana dla tego rodzaju nieruchomości. Bez niej portal odrzuci ofertę.");
        }

        // Pokoju nie da się wystawić na sprzedaż. Portale przyjmują go tylko na wynajem.
        if (request.propertyType().rentOnly() && request.transactionType() != TransactionType.RENT) {
            errors.put("transactionType", "Pokój można wystawić wyłącznie na wynajem.");
        }

        AreaRequest area = request.area();
        if (area.floorNo() != null && area.buildingFloorsCount() != null
                && area.floorNo() > area.buildingFloorsCount()) {
            errors.put("area.floorNo", "Piętro nie może być wyższe niż liczba pięter w budynku.");
        }

        if (area.usableArea() != null
                && area.usableArea().compareTo(area.totalArea()) > 0) {
            errors.put("area.usableArea", "Powierzchnia użytkowa nie może przekraczać całkowitej.");
        }

        EnergyRequest energy = request.energy();
        if (energy != null && Boolean.TRUE.equals(energy.exempt())) {
            if (isBlank(energy.exemptNote())) {
                errors.put("energy.exemptNote",
                        "Podaj podstawę zwolnienia ze świadectwa energetycznego.");
            }
            if (energy.energyPrimary() != null) {
                errors.put("energy.energyPrimary",
                        "Zaznaczono zwolnienie ze świadectwa, więc wskaźnik EP nie powinien być podany.");
            }
        }

        if (energy != null && energy.issuedAt() != null && energy.validUntil() != null
                && energy.validUntil().isBefore(energy.issuedAt())) {
            errors.put("energy.validUntil", "Data ważności nie może być wcześniejsza niż data wystawienia.");
        }

        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }
    }

    /**
     * Kolejny numer oferty w biurze, w formacie {@code RRRR/MM/NNN}.
     *
     * <p>Portal rozpoznaje ogłoszenie po tym numerze przy każdej kolejnej
     * wysyłce, więc musi być stabilny i unikalny w obrębie biura. Ale nie ma
     * powodu, żeby wpisywał go człowiek. Licznik startuje od liczby ofert
     * z bieżącego miesiąca i przesuwa się, dopóki numer jest zajęty; przy
     * jednoczesnym zapisie z dwóch sesji ostatnim zabezpieczeniem pozostaje
     * unikalny indeks {@code ux_properties_reference}.
     */
    private String nextReferenceNumber(UUID agencyId) {
        YearMonth month = YearMonth.now();
        String prefix = "%d/%02d/".formatted(month.getYear(), month.getMonthValue());

        long ordinal = properties.countByAgencyIdAndReferenceNumberStartingWith(agencyId, prefix) + 1;
        String candidate = prefix + "%03d".formatted(ordinal);

        while (properties.existsByAgencyIdAndReferenceNumber(agencyId, candidate)) {
            ordinal++;
            candidate = prefix + "%03d".formatted(ordinal);
        }

        return candidate;
    }

    /** Agent prowadzący musi być z tego samego biura co osoba dodająca ofertę. */
    private User resolveAgent(UUID agentId, User author) {
        if (agentId == null || agentId.equals(author.getId())) {
            return author;
        }

        return users.findById(agentId)
                .filter(candidate -> Objects.equals(
                        candidate.getAgency().getId(), author.getAgency().getId()))
                .orElseThrow(() -> new BusinessValidationException(
                        Map.of("agentId", "Wybrany agent nie należy do tego biura.")));
    }

    private static void applyPricing(Pricing pricing, pl.delta.crm.property.dto.PricingRequest request,
                                     BigDecimal totalArea) {
        pricing.setPriceNegotiable(orDefault(request.priceNegotiable(), Boolean.FALSE));
        pricing.setPriceIncludesRent(orDefault(request.priceIncludesRent(), Boolean.FALSE));
        pricing.setRent(request.rent(), orDefault(request.rentCurrency(), Currency.PLN));
        pricing.setDeposit(request.deposit(), orDefault(request.depositCurrency(), Currency.PLN));
        pricing.setCommissionPercent(request.commissionPercent());
        // Front zwykle przysyła cenę za m², ale gdy jej brak. Liczymy z ceny
        // i powierzchni, żeby w bazie zawsze była wartość spójna z ceną.
        pricing.setPricePerM2(request.pricePerM2() != null
                ? request.pricePerM2()
                : pricing.pricePerSquareMeter(totalArea));
    }

    private static Address buildAddress(AddressRequest request) {
        Address address = new Address(request.voivodeship(), request.city().trim());

        address.setCounty(trimToNull(request.county()));
        address.setCommune(trimToNull(request.commune()));
        address.setDistrict(trimToNull(request.district()));
        address.setStreet(trimToNull(request.street()));
        address.setBuildingNumber(trimToNull(request.buildingNumber()));
        address.setApartmentNumber(trimToNull(request.apartmentNumber()));
        address.setPostalCode(trimToNull(request.postalCode()));
        address.setCoordinates(request.latitude(), request.longitude());
        address.setHideExactAddress(orDefault(request.hideExactAddress(), Boolean.TRUE));
        address.setTerytSimc(trimToNull(request.terytSimc()));
        address.setTerytUlic(trimToNull(request.terytUlic()));
        return address;
    }

    private static void applyArea(Property property, AreaRequest request) {
        property.setUsableArea(request.usableArea());
        property.setPlotArea(request.plotArea());
        property.setRoomsCount(request.roomsCount());
        property.setBathroomsCount(request.bathroomsCount());
        property.setFloorNo(request.floorNo());
        property.setBuildingFloorsCount(request.buildingFloorsCount());
        property.setCeilingHeight(request.ceilingHeight());
    }

    private static void applyBuilding(BuildingDetails building, BuildingRequest request) {
        if (request == null) {
            return;
        }
        building.setBuildYear(request.buildYear());
        building.setBuildingType(request.buildingType());
        building.setBuildingMaterial(request.buildingMaterial());
        building.setConstructionStatus(request.constructionStatus());
        building.setWindowsType(request.windowsType());
        building.setRoofType(request.roofType());
        building.setRoofing(request.roofing());
        building.setGarretType(request.garretType());
        building.setOwnershipForm(request.ownershipForm());
        building.setSurroundings(request.surroundings());
        building.setFurnished(request.furnished());
    }

    private static void applyEnergy(EnergyCertificate certificate, EnergyRequest request) {
        if (request == null) {
            return;
        }
        certificate.setEnergyPrimary(request.energyPrimary());
        certificate.setEnergyFinal(request.energyFinal());
        certificate.setEnergyClass(request.energyClass());
        certificate.setCertificateNumber(trimToNull(request.certificateNumber()));
        certificate.setIssuedAt(request.issuedAt());
        certificate.setValidUntil(request.validUntil());
        certificate.setExempt(orDefault(request.exempt(), Boolean.FALSE));
        certificate.setExemptNote(trimToNull(request.exemptNote()));
    }

    private static void applyLand(LandDetails land, LandRequest request) {
        if (request == null) {
            return;
        }
        land.setPlotType(request.plotType());
        land.setDimensions(trimToNull(request.dimensions()));
        land.setRoadAccess(request.roadAccess());
        land.setFenced(request.fenced());
        land.setZoningPlan(trimToNull(request.zoningPlan()));
    }

    private static void applyCommercial(CommercialDetails commercial, CommercialRequest request) {
        if (request == null) {
            return;
        }
        commercial.setStructure(request.structure());
        commercial.setFlooring(request.flooring());
        commercial.setParkingType(request.parkingType());
        commercial.setOfficeSpace(request.officeSpace());
        commercial.setSocialFacilities(request.socialFacilities());
        commercial.setLoadingRamp(request.loadingRamp());
        commercial.setPowerConnectionKw(request.powerConnectionKw());
        commercial.setFloorLoadPerM2(request.floorLoadPerM2());
        commercial.setLoadingDocksCount(request.loadingDocksCount());
    }

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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Pomocnicze. Używane przez testy i przyszły moduł eksportu. */
    public static Set<PropertyType> typesRequiringRooms() {
        return Set.of(PropertyType.APARTMENT, PropertyType.HOUSE);
    }
}
