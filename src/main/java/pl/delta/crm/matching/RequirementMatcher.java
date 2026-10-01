package pl.delta.crm.matching;

import pl.delta.crm.client.requirement.ClientRequirement;
import pl.delta.crm.client.requirement.RequirementLocation;
import pl.delta.crm.property.Property;
import pl.delta.crm.property.dictionary.Currency;
import pl.delta.crm.property.dictionary.Feature;
import pl.delta.crm.property.dictionary.PropertyType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Porównuje jedno poszukiwanie z jedną ofertą i tłumaczy wynik kryterium po
 * kryterium. Agent ma zobaczyć nie tylko „pasuje", ale też dlaczego i gdzie
 * trzeba dopytać.
 *
 * <p>Transakcja i rodzaj nieruchomości są warunkiem wstępnym (sprawdza je już
 * zapytanie wybierające kandydatów), reszta ocenia się tutaj. Oferta pasuje,
 * gdy żadne kryterium nie jest {@link MatchVerdict#MISSED}; {@code NEAR}
 * i {@code UNKNOWN} nie wykluczają, tylko ostrzegają.
 *
 * <p>Klasa bez stanu i bez dostępu do bazy. Cała wiedza o dopasowaniu jest
 * w jednym miejscu i daje się przeczytać od góry do dołu.
 */
public final class RequirementMatcher {

    /** Ile ponad budżet / poza metraż uznajemy jeszcze za „prawie". */
    static final BigDecimal TOLERANCE = new BigDecimal("0.10");

    private static final Set<PropertyType> FLOOR_TYPES =
            Set.of(PropertyType.APARTMENT, PropertyType.ROOM, PropertyType.COMMERCIAL_UNIT);

    private static final Set<PropertyType> ROOMS_TYPES = Set.of(PropertyType.APARTMENT, PropertyType.HOUSE);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d.MM.yyyy");

    private RequirementMatcher() {
        // klasa narzędziowa
    }

    public static MatchResult evaluate(ClientRequirement requirement, Property property) {
        List<CriterionResult> criteria = new ArrayList<>();

        criteria.add(requirement.getPropertyTypes().contains(property.getPropertyType())
                ? CriterionResult.met(MatchCriterion.PROPERTY_TYPE)
                : CriterionResult.of(MatchCriterion.PROPERTY_TYPE, MatchVerdict.MISSED, null));

        if (!requirement.getLocations().isEmpty()) {
            criteria.add(location(requirement.getLocations(), property));
        }
        if (requirement.getPriceMin() != null || requirement.getPriceMax() != null) {
            criteria.add(price(requirement, property));
        }
        if (requirement.getAreaMin() != null || requirement.getAreaMax() != null) {
            criteria.add(range(MatchCriterion.AREA, requirement.getAreaMin(), requirement.getAreaMax(),
                    property.getTotalArea(), "m²", "oferta nie ma podanej powierzchni"));
        }
        if (ROOMS_TYPES.contains(property.getPropertyType())
                && (requirement.getRoomsMin() != null || requirement.getRoomsMax() != null)) {
            criteria.add(rooms(requirement, property));
        }
        if (FLOOR_TYPES.contains(property.getPropertyType())
                && (requirement.getFloorMin() != null || requirement.getFloorMax() != null
                    || requirement.isExcludeTopFloor())) {
            criteria.add(floor(requirement, property));
        }
        if (requirement.getMarketType() != null) {
            criteria.add(requirement.getMarketType() == property.getMarketType()
                    ? CriterionResult.met(MatchCriterion.MARKET)
                    : CriterionResult.of(MatchCriterion.MARKET, MatchVerdict.MISSED,
                            "rynek " + property.getMarketType().label().toLowerCase(Locale.ROOT)));
        }

        Map<Feature, Boolean> wanted = requirement.getFeatures();
        Set<Feature> has = property.getFeatures();
        List<Feature> required = wanted.entrySet().stream()
                .filter(Map.Entry::getValue).map(Map.Entry::getKey).sorted().toList();
        List<Feature> preferred = wanted.entrySet().stream()
                .filter(e -> !e.getValue()).map(Map.Entry::getKey).sorted().toList();

        if (!required.isEmpty()) {
            List<Feature> missing = required.stream().filter(f -> !has.contains(f)).toList();
            criteria.add(missing.isEmpty()
                    ? CriterionResult.met(MatchCriterion.REQUIRED_FEATURES)
                    : CriterionResult.of(MatchCriterion.REQUIRED_FEATURES, MatchVerdict.MISSED,
                            "brak: " + labels(missing)));
        }

        int preferredHits = 0;
        if (!preferred.isEmpty()) {
            List<Feature> present = preferred.stream().filter(has::contains).toList();
            preferredHits = present.size();
            // Mile widziane nigdy nie wykluczają. Pokazujemy tylko, ile się zgadza.
            criteria.add(CriterionResult.of(MatchCriterion.PREFERRED_FEATURES,
                    present.size() == preferred.size() ? MatchVerdict.MET : MatchVerdict.NEAR,
                    present.size() + " z " + preferred.size()
                            + (present.isEmpty() ? "" : ": " + labels(present))));
        }

        if (requirement.getMoveInDate() != null && property.getAvailableFrom() != null
                && property.getAvailableFrom().isAfter(requirement.getMoveInDate())) {
            criteria.add(CriterionResult.of(MatchCriterion.MOVE_IN, MatchVerdict.NEAR,
                    "dostępna dopiero od " + DATE.format(property.getAvailableFrom())));
        }

        return new MatchResult(criteria, preferredHits);
    }

    // --- kryteria ----------------------------------------------------------------

    /**
     * Miejscowość musi się zgadzać; dzielnica tylko wtedy, gdy klient ją podał.
     * Porównanie bez wielkości liter i polskich znaków. „Lodz" z formularza
     * i „Łódź" z adresu oferty to to samo miasto.
     */
    private static CriterionResult location(List<RequirementLocation> locations, Property property) {
        String city = normalize(property.getAddress().getCity());
        String district = normalize(property.getAddress().getDistrict());

        boolean cityMatched = false;
        for (RequirementLocation location : locations) {
            if (!normalize(location.getCity()).equals(city)) {
                continue;
            }
            cityMatched = true;
            if (location.getDistrict() == null || normalize(location.getDistrict()).equals(district)) {
                return CriterionResult.met(MatchCriterion.LOCATION);
            }
        }

        if (cityMatched && district.isEmpty()) {
            return CriterionResult.of(MatchCriterion.LOCATION, MatchVerdict.UNKNOWN,
                    "oferta nie ma podanej dzielnicy");
        }
        String where = property.getAddress().getCity()
                + (property.getAddress().getDistrict() == null ? "" : ", " + property.getAddress().getDistrict());
        return CriterionResult.of(MatchCriterion.LOCATION, MatchVerdict.MISSED, where);
    }

    /** Budżet jest w PLN. Oferty w innej walucie nie przeliczamy, tylko prosimy o sprawdzenie. */
    private static CriterionResult price(ClientRequirement requirement, Property property) {
        if (property.getPricing().getPriceCurrency() != Currency.PLN) {
            return CriterionResult.of(MatchCriterion.PRICE, MatchVerdict.UNKNOWN,
                    "cena w " + property.getPricing().getPriceCurrency().name());
        }
        return range(MatchCriterion.PRICE, requirement.getPriceMin(), requirement.getPriceMax(),
                property.getPricing().getPrice(), "zł", "oferta nie ma ceny");
    }

    private static CriterionResult range(MatchCriterion criterion, BigDecimal min, BigDecimal max,
                                         BigDecimal actual, String unit, String missingNote) {
        if (actual == null) {
            return CriterionResult.of(criterion, MatchVerdict.UNKNOWN, missingNote);
        }

        if (max != null && actual.compareTo(max) > 0) {
            BigDecimal over = deviation(actual, max);
            return CriterionResult.of(criterion,
                    over.compareTo(TOLERANCE) <= 0 ? MatchVerdict.NEAR : MatchVerdict.MISSED,
                    number(actual) + " " + unit + ". O " + percent(over) + "% powyżej");
        }
        if (min != null && actual.compareTo(min) < 0) {
            BigDecimal under = deviation(actual, min);
            return CriterionResult.of(criterion,
                    under.compareTo(TOLERANCE) <= 0 ? MatchVerdict.NEAR : MatchVerdict.MISSED,
                    number(actual) + " " + unit + ". O " + percent(under) + "% poniżej");
        }
        return CriterionResult.met(criterion);
    }

    private static CriterionResult rooms(ClientRequirement requirement, Property property) {
        Short rooms = property.getRoomsCount();
        if (rooms == null) {
            return CriterionResult.of(MatchCriterion.ROOMS, MatchVerdict.UNKNOWN, "oferta nie ma liczby pokoi");
        }
        boolean tooFew = requirement.getRoomsMin() != null && rooms < requirement.getRoomsMin();
        boolean tooMany = requirement.getRoomsMax() != null && rooms > requirement.getRoomsMax();
        return tooFew || tooMany
                ? CriterionResult.of(MatchCriterion.ROOMS, MatchVerdict.MISSED, "w ofercie " + rooms)
                : CriterionResult.met(MatchCriterion.ROOMS);
    }

    private static CriterionResult floor(ClientRequirement requirement, Property property) {
        Short floor = property.getFloorNo();
        if (floor == null) {
            return CriterionResult.of(MatchCriterion.FLOOR, MatchVerdict.UNKNOWN, "oferta nie ma podanego piętra");
        }

        String actual = floor == 0 ? "parter" : floor == -1 ? "suterena" : "piętro " + floor;
        boolean tooLow = requirement.getFloorMin() != null && floor < requirement.getFloorMin();
        boolean tooHigh = requirement.getFloorMax() != null && floor > requirement.getFloorMax();
        if (tooLow || tooHigh) {
            return CriterionResult.of(MatchCriterion.FLOOR, MatchVerdict.MISSED, actual);
        }

        if (requirement.isExcludeTopFloor()) {
            Short floors = property.getBuildingFloorsCount();
            if (floors == null) {
                return CriterionResult.of(MatchCriterion.FLOOR, MatchVerdict.UNKNOWN,
                        actual + ". Nie wiadomo, czy ostatnie");
            }
            if (floor >= floors) {
                return CriterionResult.of(MatchCriterion.FLOOR, MatchVerdict.MISSED, actual + ". Ostatnie");
            }
        }
        return CriterionResult.met(MatchCriterion.FLOOR);
    }

    // --- pomocnicze --------------------------------------------------------------

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.trim().toLowerCase(Locale.ROOT).replace('ł', 'l');
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** Względne odchylenie od granicy jako ułamek: 0.06 = 6%. Granica 0 = zawsze dużo. */
    private static BigDecimal deviation(BigDecimal value, BigDecimal limit) {
        if (limit.signum() == 0) {
            return BigDecimal.ONE;
        }
        return value.subtract(limit).abs().divide(limit, 4, RoundingMode.HALF_UP);
    }

    /** Ułamek → zaokrąglony procent do komunikatu; co najmniej 1, żeby nie pisać „o 0%". */
    private static String percent(BigDecimal fraction) {
        BigDecimal value = fraction.multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_UP);
        return value.max(BigDecimal.ONE).toPlainString();
    }

    private static String number(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL"));
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }

    private static String labels(List<Feature> features) {
        return features.stream().map(Feature::label).collect(Collectors.joining(", "));
    }
}
