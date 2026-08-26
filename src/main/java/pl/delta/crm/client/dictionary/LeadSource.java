package pl.delta.crm.client.dictionary;

import pl.delta.crm.property.dictionary.Dictionary;

/** Skąd trafił do biura klient — źródło pozyskania kontaktu. */
public enum LeadSource implements Dictionary {

    REFERRAL("Polecenie"),
    LISTING_PORTAL("Portal ogłoszeniowy"),
    PHONE_CALL("Telefon"),
    WEBSITE("Strona WWW"),
    OFFICE_VISIT("Wizyta w biurze"),
    OTHER("Inne");

    private final String label;

    LeadSource(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
