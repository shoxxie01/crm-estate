package pl.delta.crm.client.dictionary;

import pl.delta.crm.property.dictionary.Dictionary;

/**
 * Stan poszukiwania. Tylko aktywne liczą się do roli klienta (kupujący /
 * najemca) i będą brane pod uwagę przy dopasowaniu ofert. Pozostałe zostają
 * w historii — „kupił u nas w 2026" to też informacja o kliencie.
 */
public enum RequirementStatus implements Dictionary {

    ACTIVE("Aktywne"),
    /** Klient na razie się wstrzymał (czeka na kredyt, sprzedaż swojego mieszkania). */
    PAUSED("Wstrzymane"),
    FULFILLED("Zrealizowane"),
    /** Kupił gdzie indziej, zrezygnował, przestał odbierać. */
    CLOSED("Nieaktualne");

    private final String label;

    RequirementStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
