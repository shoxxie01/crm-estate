package pl.delta.crm.property.dictionary;

/** Posadzka w hali (Otodom HallDetails: Flooring). */
public enum Flooring implements Dictionary {

    DUST_FREE("Niepylna"),
    DUSTY("Pylna"),
    NONE("Brak");

    private final String label;

    Flooring(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
