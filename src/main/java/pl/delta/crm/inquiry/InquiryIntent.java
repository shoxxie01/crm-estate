package pl.delta.crm.inquiry;

import pl.delta.crm.property.dictionary.Dictionary;

/** Po co klient przychodzi z formularza — tylko te dwie opcje, najem obsługuje agent. */
public enum InquiryIntent implements Dictionary {

    /** Opisuje kryteria; przyjęcie tworzy poszukiwanie. */
    BUY("Kupno"),
    /** Opisuje nieruchomość do sprzedaży; przyjęcie tworzy klienta, ofertę agent zakłada po rozmowie. */
    SELL("Sprzedaż");

    private final String label;

    InquiryIntent(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
