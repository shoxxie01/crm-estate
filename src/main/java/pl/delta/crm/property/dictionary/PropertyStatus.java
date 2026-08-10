package pl.delta.crm.property.dictionary;

/**
 * Status oferty w CRM-ie. Nie ma odpowiednika w słownikach portali — tam
 * ogłoszenie jest po prostu aktywne albo nie. Eksport publikuje wyłącznie
 * {@link #AKTYWNA}, a przejście w każdy inny stan powinno wywołać dezaktywację
 * ogłoszenia w portalu (Otodom: Action=1).
 */
public enum PropertyStatus implements Dictionary {

    ROBOCZA("Robocza"),
    AKTYWNA("Aktywna"),
    ZAREZERWOWANA("Zarezerwowana"),
    SPRZEDANA("Sprzedana"),
    WYNAJETA("Wynajęta"),
    ARCHIWALNA("Archiwalna");

    private final String label;

    PropertyStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    public boolean publishable() {
        return this == AKTYWNA;
    }
}
