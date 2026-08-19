package pl.delta.crm.client.dictionary;

import pl.delta.crm.property.dictionary.Dictionary;

/** Skąd trafił do biura klient — źródło pozyskania kontaktu. */
public enum LeadSource implements Dictionary {

    POLECENIE("Polecenie"),
    PORTAL("Portal ogłoszeniowy"),
    TELEFON("Telefon"),
    STRONA_WWW("Strona WWW"),
    WIZYTA("Wizyta w biurze"),
    INNE("Inne");

    private final String label;

    LeadSource(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
