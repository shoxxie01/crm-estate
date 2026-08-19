package pl.delta.crm.property.dictionary;

/** Dostęp do łazienki przy wynajmie pokoju. */
public enum RoomBathroom implements Dictionary {

    OSOBNA("Osobna"),
    WSPOLDZIELONA("Współdzielona");

    private final String label;

    RoomBathroom(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
