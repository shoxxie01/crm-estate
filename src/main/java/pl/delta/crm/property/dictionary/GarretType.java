package pl.delta.crm.property.dictionary;

/** Poddasze (Otodom HouseDetails: GarretType). */
public enum GarretType implements Dictionary {

    UZYTKOWE("Użytkowe"),
    NIEUZYTKOWE("Nieużytkowe"),
    BRAK("Brak");

    private final String label;

    GarretType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
