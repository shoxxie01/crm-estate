package pl.delta.crm.property.dictionary;

/** Rodzaj garażu lub miejsca postojowego. */
public enum GarageType implements Dictionary {

    DETACHED_BRICK("Wolnostojący murowany"),
    METAL_SHED("Blaszany"),
    UNDERGROUND("Podziemny"),
    SPACE_IN_GARAGE_HALL("Miejsce w hali garażowej"),
    SURFACE_PARKING_SPACE("Miejsce postojowe naziemne");

    private final String label;

    GarageType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
