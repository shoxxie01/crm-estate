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
    APARTMENT_BLOCK("Blok"),
    TENEMENT("Kamienica"),
    APARTMENT_BUILDING("Apartamentowiec"),
    INFILL_BUILDING("Plomba"),
    LOFT("Loft"),

    // zabudowa jednorodzinna
    DETACHED_HOUSE("Dom wolnostojący"),
    SEMI_DETACHED("Bliźniak"),
    TERRACED("Szeregowiec"),
    MANOR_PALACE("Dworek / pałac"),
    FARMSTEAD("Gospodarstwo"),

    // umiejscowienie lokalu użytkowego
    IN_SHOPPING_CENTRE("W centrum handlowym"),
    IN_OFFICE_BUILDING("W biurowcu"),
    IN_PRIVATE_HOUSE("W domu prywatnym"),
    IN_HISTORIC_BUILDING("W budynku zabytkowym"),
    STANDALONE_BUILDING("Osobny obiekt");

    private final String label;

    BuildingType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
