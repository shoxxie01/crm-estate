package pl.delta.crm.property.dictionary;

/** Typ działki (Otodom TerrainDetails: Type). */
public enum PlotType implements Dictionary {

    BUILDING("Budowlana"),
    AGRICULTURAL("Rolna"),
    AGRICULTURAL_BUILDING("Rolno-budowlana"),
    RECREATIONAL("Rekreacyjna"),
    INVESTMENT("Pod inwestycję"),
    HOMESTEAD("Siedliskowa"),
    FOREST("Leśna"),
    OTHER("Inna");

    private final String label;

    PlotType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
