package pl.delta.crm.property.dictionary;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pl.delta.crm.property.dictionary.FeatureCategory.DODATKOWE;
import static pl.delta.crm.property.dictionary.FeatureCategory.MEDIA;
import static pl.delta.crm.property.dictionary.FeatureCategory.OGRODZENIE;
import static pl.delta.crm.property.dictionary.FeatureCategory.OKOLICA;
import static pl.delta.crm.property.dictionary.FeatureCategory.WYPOSAZENIE;
import static pl.delta.crm.property.dictionary.FeatureCategory.ZABEZPIECZENIA;
import static pl.delta.crm.property.dictionary.PropertyType.DOM;
import static pl.delta.crm.property.dictionary.PropertyType.DZIALKA;
import static pl.delta.crm.property.dictionary.PropertyType.HALA_MAGAZYN;
import static pl.delta.crm.property.dictionary.PropertyType.LOKAL_UZYTKOWY;
import static pl.delta.crm.property.dictionary.PropertyType.MIESZKANIE;
import static pl.delta.crm.property.dictionary.PropertyType.POKOJ;

/**
 * Cechy nieruchomości — jeden zbiór zamiast sześciu osobnych list boolean.
 *
 * <p>Suma zbiorów masek Otodom dla wszystkich typów obiektu, poszerzona o kilka
 * pozycji, których ich słownik z 2017 r. nie zna, a które są dziś standardem
 * w ogłoszeniach (komórka lokatorska, miejsce postojowe, loggia, studnia).
 *
 * <p>Każda cecha zna typy obiektu, dla których ma sens — formularz pokazuje
 * w sekcji „Cechy" tylko te pasujące do wybranego rodzaju (garaż nie pyta
 * o pralkę, mieszkanie o studnię).
 */
public enum Feature implements Dictionary {

    // --- informacje dodatkowe -------------------------------------------------
    BALKON("Balkon", DODATKOWE, MIESZKANIE, DOM, POKOJ),
    TARAS("Taras", DODATKOWE, MIESZKANIE, DOM, LOKAL_UZYTKOWY),
    LOGGIA("Loggia", DODATKOWE, MIESZKANIE, POKOJ),
    OGRODEK("Ogródek", DODATKOWE, MIESZKANIE, DOM),
    PIWNICA("Piwnica", DODATKOWE, MIESZKANIE, DOM, LOKAL_UZYTKOWY),
    STRYCH("Strych", DODATKOWE, MIESZKANIE, DOM),
    GARAZ("Garaż", DODATKOWE, MIESZKANIE, DOM),
    MIEJSCE_POSTOJOWE("Miejsce postojowe", DODATKOWE, MIESZKANIE, DOM, LOKAL_UZYTKOWY, HALA_MAGAZYN),
    KOMORKA_LOKATORSKA("Komórka lokatorska", DODATKOWE, MIESZKANIE),
    POMIESZCZENIE_UZYTKOWE("Pomieszczenie użytkowe", DODATKOWE, MIESZKANIE, DOM, LOKAL_UZYTKOWY),
    WINDA("Winda", DODATKOWE, MIESZKANIE, LOKAL_UZYTKOWY, POKOJ),
    KLIMATYZACJA("Klimatyzacja", DODATKOWE, MIESZKANIE, DOM, LOKAL_UZYTKOWY, HALA_MAGAZYN, POKOJ),
    DWUPOZIOMOWE("Dwupoziomowe", DODATKOWE, MIESZKANIE),
    ODDZIELNA_KUCHNIA("Oddzielna kuchnia", DODATKOWE, MIESZKANIE, DOM, POKOJ),
    BASEN("Basen", DODATKOWE, DOM),
    WITRYNA("Witryna", DODATKOWE, LOKAL_UZYTKOWY),
    PODJAZD_DLA_NIEPELNOSPRAWNYCH("Dostęp dla niepełnosprawnych", DODATKOWE,
            MIESZKANIE, DOM, LOKAL_UZYTKOWY, HALA_MAGAZYN, POKOJ),
    TYLKO_DLA_NIEPALACYCH("Tylko dla niepalących", DODATKOWE, MIESZKANIE, POKOJ),
    ZWIERZETA_DOZWOLONE("Zwierzęta dozwolone", DODATKOWE, MIESZKANIE, DOM, POKOJ),

    // --- zabezpieczenia -------------------------------------------------------
    DOMOFON("Domofon / wideofon", ZABEZPIECZENIA, MIESZKANIE, DOM, LOKAL_UZYTKOWY, POKOJ),
    MONITORING("Monitoring / ochrona", ZABEZPIECZENIA,
            MIESZKANIE, DOM, DZIALKA, LOKAL_UZYTKOWY, HALA_MAGAZYN, PropertyType.GARAZ, POKOJ),
    SYSTEM_ALARMOWY("System alarmowy", ZABEZPIECZENIA,
            MIESZKANIE, DOM, LOKAL_UZYTKOWY, HALA_MAGAZYN, PropertyType.GARAZ),
    DRZWI_ANTYWLAMANIOWE("Drzwi / okna antywłamaniowe", ZABEZPIECZENIA,
            MIESZKANIE, DOM, LOKAL_UZYTKOWY),
    ROLETY_ANTYWLAMANIOWE("Rolety antywłamaniowe", ZABEZPIECZENIA, MIESZKANIE, DOM, LOKAL_UZYTKOWY),
    TEREN_ZAMKNIETY("Teren zamknięty", ZABEZPIECZENIA,
            MIESZKANIE, DOM, DZIALKA, LOKAL_UZYTKOWY, HALA_MAGAZYN, PropertyType.GARAZ),

    // --- media ----------------------------------------------------------------
    PRAD("Prąd", MEDIA, DOM, DZIALKA, LOKAL_UZYTKOWY, HALA_MAGAZYN, PropertyType.GARAZ),
    PRAD_SILA("Prąd — siła (3-fazowy)", MEDIA, DOM, DZIALKA, LOKAL_UZYTKOWY, HALA_MAGAZYN),
    WODA_MIEJSKA("Woda miejska", MEDIA, DOM, DZIALKA, LOKAL_UZYTKOWY, HALA_MAGAZYN),
    STUDNIA("Studnia", MEDIA, DOM, DZIALKA),
    GAZ("Gaz", MEDIA, DOM, DZIALKA, LOKAL_UZYTKOWY, HALA_MAGAZYN),
    KANALIZACJA("Kanalizacja", MEDIA, DOM, DZIALKA, LOKAL_UZYTKOWY, HALA_MAGAZYN),
    SZAMBO("Szambo", MEDIA, DOM, DZIALKA),
    OCZYSZCZALNIA("Oczyszczalnia", MEDIA, DOM, DZIALKA),
    INTERNET("Internet", MEDIA, MIESZKANIE, DOM, LOKAL_UZYTKOWY, HALA_MAGAZYN, POKOJ),
    TELEWIZJA_KABLOWA("Telewizja kablowa", MEDIA, MIESZKANIE, DOM, POKOJ),
    TELEFON("Telefon", MEDIA, MIESZKANIE, DOM, LOKAL_UZYTKOWY),

    // --- wyposażenie ----------------------------------------------------------
    PRALKA("Pralka", WYPOSAZENIE, MIESZKANIE, DOM, POKOJ),
    ZMYWARKA("Zmywarka", WYPOSAZENIE, MIESZKANIE, DOM),
    LODOWKA("Lodówka", WYPOSAZENIE, MIESZKANIE, DOM, POKOJ),
    KUCHENKA("Kuchenka", WYPOSAZENIE, MIESZKANIE, DOM, POKOJ),
    PIEKARNIK("Piekarnik", WYPOSAZENIE, MIESZKANIE, DOM),
    TELEWIZOR("Telewizor", WYPOSAZENIE, MIESZKANIE, DOM, POKOJ),
    MEBLE("Meble", WYPOSAZENIE, MIESZKANIE, DOM, LOKAL_UZYTKOWY, POKOJ),

    // --- okolica --------------------------------------------------------------
    LAS("Las", OKOLICA, MIESZKANIE, DOM, DZIALKA, POKOJ),
    JEZIORO("Jezioro", OKOLICA, MIESZKANIE, DOM, DZIALKA, POKOJ),
    MORZE("Morze", OKOLICA, MIESZKANIE, DOM, DZIALKA, POKOJ),
    GORY("Góry", OKOLICA, MIESZKANIE, DOM, DZIALKA, POKOJ),
    OTWARTY_TEREN("Otwarty teren", OKOLICA, DOM, DZIALKA),

    // --- ogrodzenie -----------------------------------------------------------
    OGRODZENIE_MUROWANE("Murowane", OGRODZENIE, DOM, DZIALKA, HALA_MAGAZYN),
    OGRODZENIE_METALOWE("Metalowe", OGRODZENIE, DOM, DZIALKA, HALA_MAGAZYN),
    OGRODZENIE_SIATKA("Siatka", OGRODZENIE, DOM, DZIALKA, HALA_MAGAZYN),
    OGRODZENIE_DREWNIANE("Drewniane", OGRODZENIE, DOM, DZIALKA),
    OGRODZENIE_BETONOWE("Betonowe", OGRODZENIE, DOM, DZIALKA, HALA_MAGAZYN),
    OGRODZENIE_ZYWOPLOT("Żywopłot", OGRODZENIE, DOM, DZIALKA);

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

    /** Pogrupowane cechy — front rysuje z tego sekcje formularza. */
    public static Map<FeatureCategory, List<Feature>> byCategory() {
        return Arrays.stream(values())
                .collect(Collectors.groupingBy(Feature::category,
                        () -> new java.util.EnumMap<>(FeatureCategory.class),
                        Collectors.toList()));
    }
}
