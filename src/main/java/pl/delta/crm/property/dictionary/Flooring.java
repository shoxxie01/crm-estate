package pl.delta.crm.property.dictionary;

/** Posadzka w hali (Otodom HallDetails: Flooring). */
public enum Flooring implements Dictionary {

    NIEPYLNA("Niepylna"),
    PYLNA("Pylna"),
    BRAK("Brak");

    private final String label;

    Flooring(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
