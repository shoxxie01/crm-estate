package pl.delta.crm.property.dictionary;

/** Okna (Otodom: WindowsType). */
public enum WindowsType implements Dictionary {

    PLASTIKOWE("Plastikowe"),
    DREWNIANE("Drewniane"),
    ALUMINIOWE("Aluminiowe"),
    BRAK("Brak");

    private final String label;

    WindowsType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
