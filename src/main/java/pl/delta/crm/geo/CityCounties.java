package pl.delta.crm.geo;

import pl.delta.crm.property.dictionary.Voivodeship;

import java.util.Map;
import java.util.Set;

import static pl.delta.crm.property.dictionary.Voivodeship.*;

/**
 * Miasta na prawach powiatu. 66 miast, dla których powiatem jest samo miasto.
 *
 * <p>Gminy ta lista nie dotyczy: brak gminy przy mieście obsługuje ogólniejsza
 * reguła w {@link GeoService} (gmina miejska nazywa się tak jak miasto),
 * a miasta na prawach powiatu są jej szczególnym przypadkiem.
 *
 * <p><b>Po co to jest.</b> OpenStreetMap nie zwraca dla nich ani powiatu, ani
 * gminy. I nie jest to luka w danych, tylko konsekwencja tego, jak wygląda
 * podział administracyjny: granica Warszawy <i>jest</i> jednostką poziomu
 * powiatu (admin_level 6), a Nominatim opisuje ten sam obiekt jako
 * {@code city}. Nie ma więc czego dopytać. Żaden poziom {@code zoom}
 * w zapytaniu odwrotnym nie wyciągnie powiatu, bo nie istnieje osobny obiekt,
 * który by go niósł. Wiedza musi przyjść z naszej strony.
 *
 * <p><b>Dlaczego to boli.</b> Powiat jest wymagany przez Otodom i sprawdza go
 * {@code Property.readyForExport()}. Bez tej listy każda oferta w dużym mieście
 * Czyli większość ofert wychodziła z pinezki z pustym powiatem i nie
 * nadawała się do wysyłki, dopóki agent nie dopisał go ręcznie.
 *
 * <p><b>Dlaczego lista, a nie słownik z zewnątrz.</b> Zbiór jest zamknięty
 * i praktycznie niezmienny: ostatnia zmiana to odzyskanie praw powiatu przez
 * Wałbrzych w 2013 r. Pobieranie tego z TERYT-u przy każdym starcie dokładałoby
 * zależność sieciową do faktu, który zmienia się raz na dekadę.
 *
 * <p>Para województwo + nazwa, a nie sama nazwa: „Chełm" to miasto na prawach
 * powiatu w lubelskim, ale też wieś w małopolskim. Dopasowanie po samej nazwie
 * dopisywałoby wsi powiat, którego nie ma.
 */
final class CityCounties {

    private static final Map<Voivodeship, Set<String>> BY_VOIVODESHIP = Map.ofEntries(
            Map.entry(DOLNOSLASKIE, Set.of("Jelenia Góra", "Legnica", "Wałbrzych", "Wrocław")),
            Map.entry(KUJAWSKO_POMORSKIE, Set.of("Bydgoszcz", "Grudziądz", "Toruń", "Włocławek")),
            Map.entry(LUBELSKIE, Set.of("Biała Podlaska", "Chełm", "Lublin", "Zamość")),
            Map.entry(LUBUSKIE, Set.of("Gorzów Wielkopolski", "Zielona Góra")),
            Map.entry(LODZKIE, Set.of("Łódź", "Piotrków Trybunalski", "Skierniewice")),
            Map.entry(MALOPOLSKIE, Set.of("Kraków", "Nowy Sącz", "Tarnów")),
            Map.entry(MAZOWIECKIE, Set.of("Ostrołęka", "Płock", "Radom", "Siedlce", "Warszawa")),
            Map.entry(OPOLSKIE, Set.of("Opole")),
            Map.entry(PODKARPACKIE, Set.of("Krosno", "Przemyśl", "Rzeszów", "Tarnobrzeg")),
            Map.entry(PODLASKIE, Set.of("Białystok", "Łomża", "Suwałki")),
            Map.entry(POMORSKIE, Set.of("Gdańsk", "Gdynia", "Słupsk", "Sopot")),
            Map.entry(SLASKIE, Set.of(
                    "Bielsko-Biała", "Bytom", "Chorzów", "Częstochowa", "Dąbrowa Górnicza",
                    "Gliwice", "Jastrzębie-Zdrój", "Jaworzno", "Katowice", "Mysłowice",
                    "Piekary Śląskie", "Ruda Śląska", "Rybnik", "Siemianowice Śląskie",
                    "Sosnowiec", "Świętochłowice", "Tychy", "Zabrze", "Żory")),
            Map.entry(SWIETOKRZYSKIE, Set.of("Kielce")),
            Map.entry(WARMINSKO_MAZURSKIE, Set.of("Elbląg", "Olsztyn")),
            Map.entry(WIELKOPOLSKIE, Set.of("Kalisz", "Konin", "Leszno", "Poznań")),
            Map.entry(ZACHODNIOPOMORSKIE, Set.of("Koszalin", "Szczecin", "Świnoujście")));

    private CityCounties() {
    }

    /**
     * Czy ta miejscowość w tym województwie jest miastem na prawach powiatu.
     * Porównanie idzie po kluczu bez znaków diakrytycznych, bo nazwa przychodzi
     * z zewnątrz i „Lodz" ma trafiać tak samo jak „Łódź".
     */
    static boolean isCityCounty(Voivodeship voivodeship, String city) {
        if (voivodeship == null || city == null || city.isBlank()) {
            return false;
        }
        Set<String> cities = BY_VOIVODESHIP.get(voivodeship);
        if (cities == null) {
            return false;
        }
        String key = GeoService.normalize(city);
        return cities.stream().anyMatch(name -> GeoService.normalize(name).equals(key));
    }

    /** Wyłącznie na potrzeby testu pilnującego, że lista ma komplet 66 pozycji. */
    static long count() {
        return BY_VOIVODESHIP.values().stream().mapToLong(Set::size).sum();
    }
}
