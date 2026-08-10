package pl.delta.crm.property.dictionary;

/** Materiał budowy (Otodom: BuildingMaterial — wspólny dla mieszkania i domu). */
public enum BuildingMaterial implements Dictionary {

    CEGLA("Cegła"),
    WIELKA_PLYTA("Wielka płyta"),
    PUSTAK("Pustak"),
    SILIKAT("Silikat"),
    BETON("Beton"),
    BETON_KOMORKOWY("Beton komórkowy"),
    KERAMZYT("Keramzyt"),
    DREWNO("Drewno"),
    INNE("Inne");

    private final String label;

    BuildingMaterial(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
