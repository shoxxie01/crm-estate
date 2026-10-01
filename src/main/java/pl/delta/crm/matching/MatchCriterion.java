package pl.delta.crm.matching;

import pl.delta.crm.property.dictionary.Dictionary;

/** Kryteria, po których poszukiwanie porównuje się z ofertą. W kolejności wyświetlania. */
public enum MatchCriterion implements Dictionary {

    PROPERTY_TYPE("Rodzaj"),
    LOCATION("Lokalizacja"),
    PRICE("Cena"),
    AREA("Metraż"),
    ROOMS("Pokoje"),
    FLOOR("Piętro"),
    MARKET("Rynek"),
    REQUIRED_FEATURES("Cechy konieczne"),
    PREFERRED_FEATURES("Mile widziane"),
    MOVE_IN("Termin");

    private final String label;

    MatchCriterion(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
