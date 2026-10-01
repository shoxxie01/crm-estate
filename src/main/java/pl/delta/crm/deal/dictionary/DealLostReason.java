package pl.delta.crm.deal.dictionary;

import pl.delta.crm.property.dictionary.Dictionary;

/**
 * Dlaczego transakcja się nie udała. Obowiązkowe przy przeniesieniu karty do
 * „Przegranych". To z tych powodów biuro wyczyta, czy przegrywa z konkurencją,
 * czy na wycenach, których właściciele nie przyjmują.
 */
public enum DealLostReason implements Dictionary {

    OWNER_WITHDREW("Właściciel się wycofał"),
    COMPETITOR("Wybrał inne biuro"),
    PRICE_EXPECTATIONS("Nierealne oczekiwania cenowe"),
    BUYER_FINANCING("Kupujący bez finansowania"),
    MANDATE_EXPIRED("Umowa pośrednictwa wygasła"),
    NO_CONTACT("Brak kontaktu"),
    OTHER("Inny powód");

    private final String label;

    DealLostReason(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
