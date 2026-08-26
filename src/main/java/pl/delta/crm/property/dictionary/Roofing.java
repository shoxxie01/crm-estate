package pl.delta.crm.property.dictionary;

/** Pokrycie dachu (Otodom HouseDetails: Roofing) — inny wymiar niż {@link RoofType}. */
public enum Roofing implements Dictionary {

    ROOF_TILE("Dachówka"),
    SHEET_METAL("Blacha"),
    BITUMEN_FELT("Papa"),
    SHINGLE("Gont"),
    SLATE("Łupek"),
    ASBESTOS_CEMENT("Eternit"),
    THATCH("Strzecha"),
    OTHER("Inne");

    private final String label;

    Roofing(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
