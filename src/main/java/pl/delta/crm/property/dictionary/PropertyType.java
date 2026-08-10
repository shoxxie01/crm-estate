package pl.delta.crm.property.dictionary;

/**
 * Rodzaj obiektu. Decyduje o tym, które pola szczegółowe mają sens, i o tym,
 * jaki tag Details zbuduje eksport (Otodom: ObjectName).
 */
public enum PropertyType implements Dictionary {

    MIESZKANIE("Mieszkanie"),
    DOM("Dom"),
    DZIALKA("Działka"),
    LOKAL_UZYTKOWY("Lokal użytkowy"),
    HALA_MAGAZYN("Hala / magazyn"),
    GARAZ("Garaż / miejsce postojowe"),
    POKOJ("Pokój");

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
        return this == MIESZKANIE || this == DOM;
    }

    /** Pokój da się wyłącznie wynająć — portale nie przyjmują go na sprzedaż. */
    public boolean rentOnly() {
        return this == POKOJ;
    }
}
