package pl.delta.crm.deal.dictionary;

import pl.delta.crm.property.dictionary.Dictionary;

/** Stan terminu umownego. Przesunięty zostaje w historii obok nowej daty. */
public enum DeadlineStatus implements Dictionary {

    OPEN("Do dotrzymania"),
    MET("Dotrzymany"),
    MOVED("Przesunięty");

    private final String label;

    DeadlineStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
