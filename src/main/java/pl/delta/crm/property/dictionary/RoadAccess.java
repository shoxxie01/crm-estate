package pl.delta.crm.property.dictionary;

/** Dojazd (Otodom: AccessMask — dla działki, domu i hali). */
public enum RoadAccess implements Dictionary {

    ASPHALT("Asfaltowy"),
    HARDENED("Utwardzony"),
    CONCRETE("Utwardzony betonowy"),
    DIRT("Polny"),
    UNPAVED("Nieutwardzony");

    private final String label;

    RoadAccess(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
