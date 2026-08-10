package pl.delta.crm.property.dictionary;

/**
 * Rodzaj zabudowy — suma zbiorów trzech osobnych słowników Otodom.
 *
 * <p>To najlepszy przykład tego, dlaczego kody portalowe nie mogą trafić do bazy:
 * Otodom ma trzy różne słowniki {@code BuildingType} — dla mieszkania, dla domu
 * i dla lokalu użytkowego — i ta sama liczba znaczy w nich co innego
 * ({@code 2} to „dom wolnostojący" przy mieszkaniu, „szeregowiec" przy domu
 * i „w bloku" przy lokalu). Trzymamy więc jeden zbiór jednoznacznych nazw,
 * a eksport wybiera właściwy słownik na podstawie {@link PropertyType}.
 */
public enum BuildingType implements Dictionary {

    // zabudowa mieszkaniowa wielorodzinna
    BLOK("Blok"),
    KAMIENICA("Kamienica"),
    APARTAMENTOWIEC("Apartamentowiec"),
    PLOMBA("Plomba"),
    LOFT("Loft"),

    // zabudowa jednorodzinna
    DOM_WOLNOSTOJACY("Dom wolnostojący"),
    BLIZNIAK("Bliźniak"),
    SZEREGOWIEC("Szeregowiec"),
    DWOREK_PALAC("Dworek / pałac"),
    GOSPODARSTWO("Gospodarstwo"),

    // umiejscowienie lokalu użytkowego
    W_CENTRUM_HANDLOWYM("W centrum handlowym"),
    W_BIUROWCU("W biurowcu"),
    W_DOMU_PRYWATNYM("W domu prywatnym"),
    W_BUDYNKU_ZABYTKOWYM("W budynku zabytkowym"),
    OSOBNY_OBIEKT("Osobny obiekt");

    private final String label;

    BuildingType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
