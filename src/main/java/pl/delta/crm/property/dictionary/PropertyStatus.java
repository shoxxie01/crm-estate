package pl.delta.crm.property.dictionary;

/**
 * Status oferty w CRM-ie. Nie ma odpowiednika w słownikach portali. Tam
 * ogłoszenie jest po prostu aktywne albo nie. Eksport publikuje wyłącznie
 * {@link #ACTIVE}, a przejście w każdy inny stan powinno wywołać dezaktywację
 * ogłoszenia w portalu (Otodom: Action=1).
 */
public enum PropertyStatus implements Dictionary {

    DRAFT("Robocza"),
    ACTIVE("Aktywna"),
    RESERVED("Zarezerwowana"),
    SOLD("Sprzedana"),
    RENTED("Wynajęta"),
    ARCHIVED("Archiwalna");

    private final String label;

    PropertyStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    public boolean publishable() {
        return this == ACTIVE;
    }
}
