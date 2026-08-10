package pl.delta.crm.property.dictionary;

/** Typ działki (Otodom TerrainDetails: Type). */
public enum PlotType implements Dictionary {

    BUDOWLANA("Budowlana"),
    ROLNA("Rolna"),
    ROLNO_BUDOWLANA("Rolno-budowlana"),
    REKREACYJNA("Rekreacyjna"),
    POD_INWESTYCJE("Pod inwestycję"),
    SIEDLISKOWA("Siedliskowa"),
    LESNA("Leśna"),
    INNA("Inna");

    private final String label;

    PlotType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
