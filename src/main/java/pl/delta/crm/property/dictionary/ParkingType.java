package pl.delta.crm.property.dictionary;

/** Nawierzchnia parkingu przy hali lub obiekcie (Otodom HallDetails: ParkingType). */
public enum ParkingType implements Dictionary {

    ASFALTOWY("Asfaltowy"),
    BETONOWY("Betonowy"),
    KOSTKA_BRUKOWA("Kostka brukowa"),
    UTWARDZONY("Utwardzony"),
    NIEUTWARDZONY("Nieutwardzony"),
    BRAK("Brak");

    private final String label;

    ParkingType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
