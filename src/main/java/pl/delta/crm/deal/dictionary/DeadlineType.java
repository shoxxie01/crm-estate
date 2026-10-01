package pl.delta.crm.deal.dictionary;

import pl.delta.crm.property.dictionary.Dictionary;

/** Rodzaj terminu umownego. Data, której biuro musi pilnować, a nie spotkanie. */
public enum DeadlineType implements Dictionary {

    MANDATE_END("Koniec umowy pośrednictwa"),
    EXCLUSIVITY_END("Koniec wyłączności"),
    RESERVATION_END("Koniec rezerwacji"),
    FINAL_CONTRACT("Termin umowy końcowej"),
    MORTGAGE_DECISION("Decyzja kredytowa"),
    NOTARY_DEED("Akt notarialny"),
    HANDOVER("Wydanie nieruchomości"),
    OTHER("Inny termin");

    private final String label;

    DeadlineType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
