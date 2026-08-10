package pl.delta.crm.property.dictionary;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static pl.delta.crm.property.dictionary.FeatureCategory.DODATKOWE;
import static pl.delta.crm.property.dictionary.FeatureCategory.MEDIA;
import static pl.delta.crm.property.dictionary.FeatureCategory.OGRODZENIE;
import static pl.delta.crm.property.dictionary.FeatureCategory.OKOLICA;
import static pl.delta.crm.property.dictionary.FeatureCategory.WYPOSAZENIE;
import static pl.delta.crm.property.dictionary.FeatureCategory.ZABEZPIECZENIA;

/**
 * Cechy nieruchomości — jeden zbiór zamiast sześciu osobnych list boolean.
 *
 * <p>Suma zbiorów masek Otodom dla wszystkich typów obiektu, poszerzona o kilka
 * pozycji, których ich słownik z 2017 r. nie zna, a które są dziś standardem
 * w ogłoszeniach (komórka lokatorska, miejsce postojowe, loggia, studnia).
 * Nadmiar nie szkodzi: eksport pomija cechy, których dany portal nie obsługuje,
 * natomiast brak cechy w modelu oznacza bezpowrotnie utraconą informację.
 */
public enum Feature implements Dictionary {

    // --- informacje dodatkowe -------------------------------------------------
    BALKON("Balkon", DODATKOWE),
    TARAS("Taras", DODATKOWE),
    LOGGIA("Loggia", DODATKOWE),
    OGRODEK("Ogródek", DODATKOWE),
    PIWNICA("Piwnica", DODATKOWE),
    STRYCH("Strych", DODATKOWE),
    GARAZ("Garaż", DODATKOWE),
    MIEJSCE_POSTOJOWE("Miejsce postojowe", DODATKOWE),
    KOMORKA_LOKATORSKA("Komórka lokatorska", DODATKOWE),
    POMIESZCZENIE_UZYTKOWE("Pomieszczenie użytkowe", DODATKOWE),
    WINDA("Winda", DODATKOWE),
    KLIMATYZACJA("Klimatyzacja", DODATKOWE),
    DWUPOZIOMOWE("Dwupoziomowe", DODATKOWE),
    ODDZIELNA_KUCHNIA("Oddzielna kuchnia", DODATKOWE),
    BASEN("Basen", DODATKOWE),
    WITRYNA("Witryna", DODATKOWE),
    PODJAZD_DLA_NIEPELNOSPRAWNYCH("Dostęp dla niepełnosprawnych", DODATKOWE),
    TYLKO_DLA_NIEPALACYCH("Tylko dla niepalących", DODATKOWE),
    ZWIERZETA_DOZWOLONE("Zwierzęta dozwolone", DODATKOWE),

    // --- zabezpieczenia -------------------------------------------------------
    DOMOFON("Domofon / wideofon", ZABEZPIECZENIA),
    MONITORING("Monitoring / ochrona", ZABEZPIECZENIA),
    SYSTEM_ALARMOWY("System alarmowy", ZABEZPIECZENIA),
    DRZWI_ANTYWLAMANIOWE("Drzwi / okna antywłamaniowe", ZABEZPIECZENIA),
    ROLETY_ANTYWLAMANIOWE("Rolety antywłamaniowe", ZABEZPIECZENIA),
    TEREN_ZAMKNIETY("Teren zamknięty", ZABEZPIECZENIA),

    // --- media ----------------------------------------------------------------
    PRAD("Prąd", MEDIA),
    PRAD_SILA("Prąd — siła (3-fazowy)", MEDIA),
    WODA_MIEJSKA("Woda miejska", MEDIA),
    STUDNIA("Studnia", MEDIA),
    GAZ("Gaz", MEDIA),
    KANALIZACJA("Kanalizacja", MEDIA),
    SZAMBO("Szambo", MEDIA),
    OCZYSZCZALNIA("Oczyszczalnia", MEDIA),
    INTERNET("Internet", MEDIA),
    TELEWIZJA_KABLOWA("Telewizja kablowa", MEDIA),
    TELEFON("Telefon", MEDIA),

    // --- wyposażenie ----------------------------------------------------------
    PRALKA("Pralka", WYPOSAZENIE),
    ZMYWARKA("Zmywarka", WYPOSAZENIE),
    LODOWKA("Lodówka", WYPOSAZENIE),
    KUCHENKA("Kuchenka", WYPOSAZENIE),
    PIEKARNIK("Piekarnik", WYPOSAZENIE),
    TELEWIZOR("Telewizor", WYPOSAZENIE),
    MEBLE("Meble", WYPOSAZENIE),

    // --- okolica --------------------------------------------------------------
    LAS("Las", OKOLICA),
    JEZIORO("Jezioro", OKOLICA),
    MORZE("Morze", OKOLICA),
    GORY("Góry", OKOLICA),
    OTWARTY_TEREN("Otwarty teren", OKOLICA),

    // --- ogrodzenie -----------------------------------------------------------
    OGRODZENIE_MUROWANE("Murowane", OGRODZENIE),
    OGRODZENIE_METALOWE("Metalowe", OGRODZENIE),
    OGRODZENIE_SIATKA("Siatka", OGRODZENIE),
    OGRODZENIE_DREWNIANE("Drewniane", OGRODZENIE),
    OGRODZENIE_BETONOWE("Betonowe", OGRODZENIE),
    OGRODZENIE_ZYWOPLOT("Żywopłot", OGRODZENIE);

    private final String label;
    private final FeatureCategory category;

    Feature(String label, FeatureCategory category) {
        this.label = label;
        this.category = category;
    }

    @Override
    public String label() {
        return label;
    }

    public FeatureCategory category() {
        return category;
    }

    /** Pogrupowane cechy — front rysuje z tego sekcje formularza. */
    public static Map<FeatureCategory, List<Feature>> byCategory() {
        return Arrays.stream(values())
                .collect(Collectors.groupingBy(Feature::category,
                        () -> new java.util.EnumMap<>(FeatureCategory.class),
                        Collectors.toList()));
    }
}
