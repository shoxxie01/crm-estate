package pl.delta.crm.property.dictionary;

/** Kształt dachu (Otodom HouseDetails: RoofType). */
public enum RoofType implements Dictionary {

    PLASKI("Płaski"),
    SKOSNY("Skośny"),
    BRAK("Brak");

    private final String label;

    RoofType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
