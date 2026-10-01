package pl.delta.crm.property.dictionary;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pl.delta.crm.property.dictionary.FeatureCategory.ADDITIONAL;
import static pl.delta.crm.property.dictionary.FeatureCategory.UTILITIES;
import static pl.delta.crm.property.dictionary.FeatureCategory.FENCING;
import static pl.delta.crm.property.dictionary.FeatureCategory.NEIGHBOURHOOD;
import static pl.delta.crm.property.dictionary.FeatureCategory.EQUIPMENT;
import static pl.delta.crm.property.dictionary.FeatureCategory.SECURITY;
import static pl.delta.crm.property.dictionary.PropertyType.HOUSE;
import static pl.delta.crm.property.dictionary.PropertyType.PLOT;
import static pl.delta.crm.property.dictionary.PropertyType.HALL_WAREHOUSE;
import static pl.delta.crm.property.dictionary.PropertyType.COMMERCIAL_UNIT;
import static pl.delta.crm.property.dictionary.PropertyType.APARTMENT;
import static pl.delta.crm.property.dictionary.PropertyType.ROOM;

/**
 * Cechy nieruchomości. Jeden zbiór zamiast sześciu osobnych list boolean.
 *
 * <p>Suma zbiorów masek Otodom dla wszystkich typów obiektu, poszerzona o kilka
 * pozycji, których ich słownik z 2017 r. nie zna, a które są dziś standardem
 * w ogłoszeniach (komórka lokatorska, miejsce postojowe, loggia, studnia).
 *
 * <p>Każda cecha zna typy obiektu, dla których ma sens. Formularz pokazuje
 * w sekcji „Cechy" tylko te pasujące do wybranego rodzaju (garaż nie pyta
 * o pralkę, mieszkanie o studnię).
 */
public enum Feature implements Dictionary {

    // --- informacje dodatkowe -------------------------------------------------
    BALCONY("Balkon", ADDITIONAL, APARTMENT, HOUSE, ROOM),
    TERRACE("Taras", ADDITIONAL, APARTMENT, HOUSE, COMMERCIAL_UNIT),
    LOGGIA("Loggia", ADDITIONAL, APARTMENT, ROOM),
    GARDEN("Ogródek", ADDITIONAL, APARTMENT, HOUSE),
    BASEMENT("Piwnica", ADDITIONAL, APARTMENT, HOUSE, COMMERCIAL_UNIT),
    ATTIC("Strych", ADDITIONAL, APARTMENT, HOUSE),
    GARAGE("Garaż", ADDITIONAL, APARTMENT, HOUSE),
    PARKING_SPACE("Miejsce postojowe", ADDITIONAL, APARTMENT, HOUSE, COMMERCIAL_UNIT, HALL_WAREHOUSE),
    STORAGE_ROOM("Komórka lokatorska", ADDITIONAL, APARTMENT),
    UTILITY_ROOM("Pomieszczenie użytkowe", ADDITIONAL, APARTMENT, HOUSE, COMMERCIAL_UNIT),
    ELEVATOR("Winda", ADDITIONAL, APARTMENT, COMMERCIAL_UNIT, ROOM),
    AIR_CONDITIONING("Klimatyzacja", ADDITIONAL, APARTMENT, HOUSE, COMMERCIAL_UNIT, HALL_WAREHOUSE, ROOM),
    TWO_LEVEL("Dwupoziomowe", ADDITIONAL, APARTMENT),
    SEPARATE_KITCHEN("Oddzielna kuchnia", ADDITIONAL, APARTMENT, HOUSE, ROOM),
    SWIMMING_POOL("Basen", ADDITIONAL, HOUSE),
    SHOP_WINDOW("Witryna", ADDITIONAL, COMMERCIAL_UNIT),
    WHEELCHAIR_ACCESS("Dostęp dla niepełnosprawnych", ADDITIONAL,
            APARTMENT, HOUSE, COMMERCIAL_UNIT, HALL_WAREHOUSE, ROOM),
    NON_SMOKERS_ONLY("Tylko dla niepalących", ADDITIONAL, APARTMENT, ROOM),
    PETS_ALLOWED("Zwierzęta dozwolone", ADDITIONAL, APARTMENT, HOUSE, ROOM),

    // --- zabezpieczenia -------------------------------------------------------
    INTERCOM("Domofon / wideofon", SECURITY, APARTMENT, HOUSE, COMMERCIAL_UNIT, ROOM),
    CCTV_SECURITY("Monitoring / ochrona", SECURITY,
            APARTMENT, HOUSE, PLOT, COMMERCIAL_UNIT, HALL_WAREHOUSE, PropertyType.GARAGE, ROOM),
    ALARM_SYSTEM("System alarmowy", SECURITY,
            APARTMENT, HOUSE, COMMERCIAL_UNIT, HALL_WAREHOUSE, PropertyType.GARAGE),
    ANTI_BURGLARY_DOORS("Drzwi / okna antywłamaniowe", SECURITY,
            APARTMENT, HOUSE, COMMERCIAL_UNIT),
    ANTI_BURGLARY_SHUTTERS("Rolety antywłamaniowe", SECURITY, APARTMENT, HOUSE, COMMERCIAL_UNIT),
    GATED_AREA("Teren zamknięty", SECURITY,
            APARTMENT, HOUSE, PLOT, COMMERCIAL_UNIT, HALL_WAREHOUSE, PropertyType.GARAGE),

    // --- media ----------------------------------------------------------------
    ELECTRICITY("Prąd", UTILITIES, HOUSE, PLOT, COMMERCIAL_UNIT, HALL_WAREHOUSE, PropertyType.GARAGE),
    THREE_PHASE_POWER("Prąd. Siła (3-fazowy)", UTILITIES, HOUSE, PLOT, COMMERCIAL_UNIT, HALL_WAREHOUSE),
    MUNICIPAL_WATER("Woda miejska", UTILITIES, HOUSE, PLOT, COMMERCIAL_UNIT, HALL_WAREHOUSE),
    WELL("Studnia", UTILITIES, HOUSE, PLOT),
    GAS("Gaz", UTILITIES, HOUSE, PLOT, COMMERCIAL_UNIT, HALL_WAREHOUSE),
    SEWERAGE("Kanalizacja", UTILITIES, HOUSE, PLOT, COMMERCIAL_UNIT, HALL_WAREHOUSE),
    SEPTIC_TANK("Szambo", UTILITIES, HOUSE, PLOT),
    SEWAGE_TREATMENT("Oczyszczalnia", UTILITIES, HOUSE, PLOT),
    INTERNET("Internet", UTILITIES, APARTMENT, HOUSE, COMMERCIAL_UNIT, HALL_WAREHOUSE, ROOM),
    CABLE_TV("Telewizja kablowa", UTILITIES, APARTMENT, HOUSE, ROOM),
    PHONE_LINE("Telefon", UTILITIES, APARTMENT, HOUSE, COMMERCIAL_UNIT),

    // --- wyposażenie ----------------------------------------------------------
    WASHING_MACHINE("Pralka", EQUIPMENT, APARTMENT, HOUSE, ROOM),
    DISHWASHER("Zmywarka", EQUIPMENT, APARTMENT, HOUSE),
    FRIDGE("Lodówka", EQUIPMENT, APARTMENT, HOUSE, ROOM),
    STOVE("Kuchenka", EQUIPMENT, APARTMENT, HOUSE, ROOM),
    OVEN("Piekarnik", EQUIPMENT, APARTMENT, HOUSE),
    TV_SET("Telewizor", EQUIPMENT, APARTMENT, HOUSE, ROOM),
    FURNITURE("Meble", EQUIPMENT, APARTMENT, HOUSE, COMMERCIAL_UNIT, ROOM),

    // --- okolica --------------------------------------------------------------
    FOREST("Las", NEIGHBOURHOOD, APARTMENT, HOUSE, PLOT, ROOM),
    LAKE("Jezioro", NEIGHBOURHOOD, APARTMENT, HOUSE, PLOT, ROOM),
    SEA("Morze", NEIGHBOURHOOD, APARTMENT, HOUSE, PLOT, ROOM),
    MOUNTAINS("Góry", NEIGHBOURHOOD, APARTMENT, HOUSE, PLOT, ROOM),
    OPEN_AREA("Otwarty teren", NEIGHBOURHOOD, HOUSE, PLOT),

    // --- ogrodzenie -----------------------------------------------------------
    FENCE_BRICK("Murowane", FENCING, HOUSE, PLOT, HALL_WAREHOUSE),
    FENCE_METAL("Metalowe", FENCING, HOUSE, PLOT, HALL_WAREHOUSE),
    FENCE_MESH("Siatka", FENCING, HOUSE, PLOT, HALL_WAREHOUSE),
    FENCE_WOOD("Drewniane", FENCING, HOUSE, PLOT),
    FENCE_CONCRETE("Betonowe", FENCING, HOUSE, PLOT, HALL_WAREHOUSE),
    FENCE_HEDGE("Żywopłot", FENCING, HOUSE, PLOT);

    private final String label;
    private final FeatureCategory category;
    private final Set<PropertyType> types;

    Feature(String label, FeatureCategory category, PropertyType... types) {
        this.label = label;
        this.category = category;
        this.types = Set.of(types);
    }

    @Override
    public String label() {
        return label;
    }

    public FeatureCategory category() {
        return category;
    }

    /** Typy obiektu, dla których ta cecha ma sens. */
    public Set<PropertyType> types() {
        return types;
    }

    /** Pogrupowane cechy. Front rysuje z tego sekcje formularza. */
    public static Map<FeatureCategory, List<Feature>> byCategory() {
        return Arrays.stream(values())
                .collect(Collectors.groupingBy(Feature::category,
                        () -> new java.util.EnumMap<>(FeatureCategory.class),
                        Collectors.toList()));
    }
}
