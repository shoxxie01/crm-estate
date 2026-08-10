package pl.delta.crm.property.dictionary;

/** Pokrycie dachu (Otodom HouseDetails: Roofing) — inny wymiar niż {@link RoofType}. */
public enum Roofing implements Dictionary {

    DACHOWKA("Dachówka"),
    BLACHA("Blacha"),
    PAPA("Papa"),
    GONT("Gont"),
    LUPEK("Łupek"),
    ETERNIT("Eternit"),
    STRZECHA("Strzecha"),
    INNE("Inne");

    private final String label;

    Roofing(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
