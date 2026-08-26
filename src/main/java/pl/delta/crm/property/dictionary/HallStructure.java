package pl.delta.crm.property.dictionary;

/** Konstrukcja hali (Otodom HallDetails: Structure). */
public enum HallStructure implements Dictionary {

    STEEL("Stalowa"),
    BRICK("Murowana"),
    WOODEN("Drewniana"),
    GLASS("Szklana"),
    TENT("Namiotowa"),
    CANOPY("Wiata");

    private final String label;

    HallStructure(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
