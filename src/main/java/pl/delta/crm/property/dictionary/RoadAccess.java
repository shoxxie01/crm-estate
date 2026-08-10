package pl.delta.crm.property.dictionary;

/** Dojazd (Otodom: AccessMask — dla działki, domu i hali). */
public enum RoadAccess implements Dictionary {

    ASFALTOWY("Asfaltowy"),
    UTWARDZONY("Utwardzony"),
    BETONOWY("Utwardzony betonowy"),
    POLNY("Polny"),
    NIEUTWARDZONY("Nieutwardzony");

    private final String label;

    RoadAccess(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
