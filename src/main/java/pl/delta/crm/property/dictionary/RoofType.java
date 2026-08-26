package pl.delta.crm.property.dictionary;

/** Kształt dachu (Otodom HouseDetails: RoofType). */
public enum RoofType implements Dictionary {

    FLAT("Płaski"),
    PITCHED("Skośny"),
    NONE("Brak");

    private final String label;

    RoofType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
