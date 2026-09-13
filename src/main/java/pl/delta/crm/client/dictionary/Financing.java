package pl.delta.crm.client.dictionary;

import pl.delta.crm.property.dictionary.Dictionary;

/**
 * Sposób finansowania zakupu. Dla agenta to przede wszystkim miara gotowości:
 * klient z gotówką albo z decyzją kredytową może podpisać umowę od ręki,
 * klient „na kredyt" dopiero zaczyna procedurę w banku.
 */
public enum Financing implements Dictionary {

    CASH("Gotówka"),
    MORTGAGE("Kredyt"),
    MORTGAGE_APPROVED("Kredyt z decyzją"),
    CASH_AND_MORTGAGE("Gotówka + kredyt");

    private final String label;

    Financing(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
