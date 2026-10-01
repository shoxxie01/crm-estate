package pl.delta.crm.property.dictionary;

/** Materiał budowy (Otodom: BuildingMaterial. Wspólny dla mieszkania i domu). */
public enum BuildingMaterial implements Dictionary {

    BRICK("Cegła"),
    PRECAST_SLAB("Wielka płyta"),
    HOLLOW_BLOCK("Pustak"),
    SAND_LIME_BRICK("Silikat"),
    CONCRETE("Beton"),
    AERATED_CONCRETE("Beton komórkowy"),
    EXPANDED_CLAY("Keramzyt"),
    WOOD("Drewno"),
    OTHER("Inne");

    private final String label;

    BuildingMaterial(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
