package pl.delta.crm.property.dictionary;

/**
 * Rodzaj obiektu. Decyduje o tym, które pola szczegółowe mają sens, i o tym,
 * jaki tag Details zbuduje eksport (Otodom: ObjectName).
 */
public enum PropertyType implements Dictionary {

    APARTMENT("Mieszkanie"),
    HOUSE("Dom"),
    PLOT("Działka"),
    COMMERCIAL_UNIT("Lokal użytkowy"),
    HALL_WAREHOUSE("Hala / magazyn"),
    GARAGE("Garaż / miejsce postojowe"),
    ROOM("Pokój");

    private final String label;

    PropertyType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    /** Otodom wymaga liczby pokoi dla mieszkania i domu — bez niej odrzuca ofertę. */
    public boolean requiresRoomsCount() {
        return this == APARTMENT || this == HOUSE;
    }

    /** Pokój da się wyłącznie wynająć — portale nie przyjmują go na sprzedaż. */
    public boolean rentOnly() {
        return this == ROOM;
    }

    /**
     * Czy typ wymaga świadectwa energetycznego do publikacji. Działka nie ma
     * budynku, a garaż i pojedynczy pokój są z obowiązku zwolnione — dla nich
     * brak świadectwa nie może blokować eksportu.
     */
    public boolean requiresEnergyCertificate() {
        return this != PLOT && this != GARAGE && this != ROOM;
    }
}
