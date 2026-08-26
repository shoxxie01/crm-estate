package pl.delta.crm.property.dictionary;

/** Nawierzchnia parkingu przy hali lub obiekcie (Otodom HallDetails: ParkingType). */
public enum ParkingType implements Dictionary {

    ASPHALT("Asfaltowy"),
    CONCRETE("Betonowy"),
    PAVING_STONE("Kostka brukowa"),
    HARDENED("Utwardzony"),
    UNPAVED("Nieutwardzony"),
    NONE("Brak");

    private final String label;

    ParkingType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
