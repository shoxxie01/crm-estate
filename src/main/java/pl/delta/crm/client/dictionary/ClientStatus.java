package pl.delta.crm.client.dictionary;

import pl.delta.crm.property.dictionary.Dictionary;

/**
 * Stan współpracy z klientem. Archiwalny zostaje w bazie (historia zleceń,
 * powiązane oferty), ale domyślnie znika z listy roboczej.
 */
public enum ClientStatus implements Dictionary {

    AKTYWNY("Aktywny"),
    ARCHIWALNY("Archiwalny");

    private final String label;

    ClientStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
