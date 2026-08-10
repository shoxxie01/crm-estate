package pl.delta.crm.property.dictionary;

/** Konstrukcja hali (Otodom HallDetails: Structure). */
public enum HallStructure implements Dictionary {

    STALOWA("Stalowa"),
    MUROWANA("Murowana"),
    DREWNIANA("Drewniana"),
    SZKLANA("Szklana"),
    NAMIOTOWA("Namiotowa"),
    WIATA("Wiata");

    private final String label;

    HallStructure(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
