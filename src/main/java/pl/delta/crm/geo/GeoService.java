package pl.delta.crm.geo;

import org.springframework.stereotype.Service;
import pl.delta.crm.geo.dto.GeoLocation;
import pl.delta.crm.geo.dto.GeoQuery;
import pl.delta.crm.property.dictionary.Voivodeship;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tłumaczy między adresem w rozumieniu CRM-u a geokoderem.
 *
 * <p>Cała wiedza o tym, jak Nominatim nazywa polskie poziomy administracyjne,
 * siedzi tutaj — {@link NominatimClient} tylko wykonuje żądania, a kontroler
 * tylko je przyjmuje.
 */
@Service
public class GeoService {

    /** Ile propozycji zwracamy przy szukaniu po adresie. */
    private static final int SEARCH_LIMIT = 5;

    /** Zmiana wielkości liter w „ł"/„Ł" i „ś"/„Ś" zależy od lokalizacji. */
    private static final Locale POLISH = Locale.forLanguageTag("pl");

    /**
     * Nazwy województw sprowadzone do postaci bez znaków diakrytycznych i bez
     * myślników — po takim kluczu porównujemy to, co przyszło z Nominatim.
     * Odpowiedź bywa „województwo kujawsko-pomorskie", „Kujawsko-Pomorskie"
     * albo (gdy padnie Accept-Language) „Kuyavian-Pomeranian Voivodeship";
     * dwa pierwsze warianty trafiają w słownik, trzeci nie — i wtedy pole
     * zostaje puste, zamiast zgadywać.
     */
    private static final Map<String, Voivodeship> BY_NAME = java.util.Arrays
            .stream(Voivodeship.values())
            .collect(Collectors.toMap(v -> normalize(v.label()), v -> v));

    private final NominatimClient client;

    public GeoService(NominatimClient client) {
        this.client = client;
    }

    /** Adres pod pinezką. {@code null}, gdy w tym punkcie nie ma nic do opisania. */
    public GeoLocation reverse(double latitude, double longitude) {
        return client.reverse(latitude, longitude).stream()
                .findFirst()
                .map(GeoService::toLocation)
                .orElse(null);
    }

    /**
     * Punkty pasujące do adresu z formularza, od najlepiej dopasowanego.
     *
     * <p>Zapytanie budujemy tekstowo, a nie przez parametry strukturalne
     * Nominatim, z dwóch powodów: wyszukiwanie strukturalne nie ma pola na
     * dzielnicę, a przy literówce w nazwie ulicy nie zwraca nic zamiast trafić
     * chociaż w miejscowość. Agent wpisujący adres ze słuchu potrzebuje tego
     * drugiego zachowania.
     */
    public List<GeoLocation> search(GeoQuery query) {
        return client.search(freeFormQuery(query), SEARCH_LIMIT).stream()
                .map(GeoService::toLocation)
                .toList();
    }

    static String freeFormQuery(GeoQuery query) {
        List<String> parts = new ArrayList<>();

        String street = trimToNull(query.street());
        if (street != null) {
            String number = trimToNull(query.buildingNumber());
            parts.add(number == null ? street : street + " " + number);
        } else {
            // Dzielnicę dokładamy tylko wtedy, gdy nie ma ulicy. Przy podanej
            // ulicy jest zbędna, a potrafi zaszkodzić: ta sama ulica bywa
            // przypisana w OSM do sąsiedniej jednostki i para „ulica +
            // dzielnica" przestaje pasować do czegokolwiek.
            String district = trimToNull(query.district());
            if (district != null) {
                parts.add(district);
            }
        }

        String city = trimToNull(query.city());
        String postalCode = trimToNull(query.postalCode());
        if (city != null) {
            parts.add(postalCode == null ? city : postalCode + " " + city);
        } else if (postalCode != null) {
            parts.add(postalCode);
        }

        if (query.voivodeship() != null) {
            parts.add("województwo " + query.voivodeship().label());
        }

        parts.add("Polska");
        return String.join(", ", parts);
    }

    /* Pakietowo-prywatne, a nie prywatne — całe mapowanie nazw OSM na nasze pola
       sprawdza GeoServiceTest bez wychodzenia do sieci. */
    static GeoLocation toLocation(NominatimPlace place) {
        NominatimPlace.Address address = place.address();
        if (address == null) {
            return new GeoLocation(decimal(place.lat()), decimal(place.lon()),
                    place.displayName(), null, null, null, null, null, null, null, null);
        }

        Voivodeship voivodeship = voivodeship(address.state());
        String city = city(address);
        String county = stripPrefix(address.county(), "powiat");
        String commune = stripPrefix(address.municipality(), "gmina");

        // Miasta na prawach powiatu są same swoim powiatem, a OSM nie ma dla nich
        // osobnego obiektu, z którego dałoby się to odczytać — szczegóły
        // w CityCounties.
        if (county == null && CityCounties.isCityCounty(voivodeship, city)) {
            county = city;
        }

        // OSM niesie powiat w formie zdaniowej („powiat puławski"), więc po
        // zdjęciu rodzajnika zostaje sama mała litera. Podnosimy ją na końcu,
        // po obu ścieżkach naraz, żeby wpis z pinezki wyglądał tak samo jak
        // wpisany ręcznie — formularz robi to samo (`titleCase`) przy
        // opuszczeniu pola.
        county = capitalize(county);

        /*
         * Gmina miejska: brak `municipality` przy mieście znaczy, że granica
         * gminy pokrywa się z granicą miasta, więc OSM ma jeden obiekt i opisuje
         * go jako `city`/`town` — dokładnie ten sam mechanizm co przy miastach
         * na prawach powiatu, tylko szczebel niżej. Gmina nazywa się wtedy tak
         * jak miasto (Puławy, Zakopane, Świdnik).
         *
         * Celowo tylko dla miast: przy wsi brak `municipality` znaczy dziurę
         * w danych, a nie że wieś jest gminą — wpisanie jej nazwy jako gminy
         * byłoby zgadywaniem. Gminy miejsko-wiejskie mają własny obiekt
         * i wchodzą wyżej, wartością z OSM.
         */
        if (commune == null) {
            commune = firstNotBlank(address.city(), address.town());
        }

        return new GeoLocation(
                decimal(place.lat()),
                decimal(place.lon()),
                place.displayName(),
                voivodeship,
                county,
                commune,
                city,
                district(address),
                trimToNull(address.road()),
                trimToNull(address.houseNumber()),
                trimToNull(address.postcode()));
    }

    /**
     * Miejscowość. Gmina jest ostatnią deską ratunku — jej nazwa pochodzi od
     * siedziby, więc „gmina Piaseczno" daje „Piaseczno" i zwykle trafia, ale
     * tylko wtedy, gdy Nominatim nie podał nic dokładniejszego.
     */
    private static String city(NominatimPlace.Address address) {
        return firstNotBlank(
                address.city(),
                address.town(),
                address.village(),
                address.hamlet(),
                stripPrefix(address.municipality(), "gmina"));
    }

    private static String district(NominatimPlace.Address address) {
        return firstNotBlank(
                address.cityDistrict(),
                address.borough(),
                address.suburb(),
                address.quarter());
    }

    private static Voivodeship voivodeship(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }
        String key = normalize(state);
        if (key.startsWith("wojewodztwo")) {
            key = key.substring("wojewodztwo".length());
        }
        return BY_NAME.get(key);
    }

    /**
     * Zdejmuje rodzajnik, który Nominatim trzyma w nazwie („powiat wołomiński",
     * „gmina Lesznowola"). W formularzu etykieta pola mówi już, co to za
     * jednostka, a portale oczekują samej nazwy.
     */
    private static String stripPrefix(String value, String prefix) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return trimToNull(trimmed.replaceFirst("(?i)^" + prefix + "\\s+", ""));
    }

    /**
     * Każde słowo z wielkiej litery, reszta mała — jak `titleCase` na froncie.
     *
     * <p>Granicą słowa jest spacja i myślnik, bo nazwy powiatów bywają
     * dwuczłonowe („warszawski zachodni") i łączone („Bielsko-Biała"). Reguła
     * jest bezpieczna akurat dla powiatów: nazywają się przymiotnikowo i nie
     * mają w środku przyimków, które w polszczyźnie zostają małe — dlatego
     * <b>nie</b> stosujemy jej do gminy, gdzie „Nowe Miasto nad Pilicą"
     * wyszłoby z błędnym „Nad".
     */
    private static String capitalize(String value) {
        if (value == null) {
            return null;
        }

        String lower = value.toLowerCase(POLISH);
        StringBuilder result = new StringBuilder(lower.length());
        boolean wordStart = true;
        for (int i = 0; i < lower.length(); i++) {
            char character = lower.charAt(i);
            result.append(wordStart ? Character.toUpperCase(character) : character);
            wordStart = character == ' ' || character == '-';
        }
        return result.toString();
    }

    /** Bez znaków diakrytycznych, bez myślników, małymi literami. */
    static String normalize(String value) {
        String stripped = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                // „ł" jest w Unicode osobną literą, a nie „l" z ogonkiem,
                // więc normalizacja NFD go nie rozkłada.
                .replace('ł', 'l')
                .replace('Ł', 'L')
                .toLowerCase(Locale.ROOT);
        return stripped.replaceAll("[^a-z]", "");
    }

    private static String firstNotBlank(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private static BigDecimal decimal(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
