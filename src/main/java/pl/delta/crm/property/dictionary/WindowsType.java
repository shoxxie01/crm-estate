package pl.delta.crm.property.dictionary;

/** Okna (Otodom: WindowsType). */
public enum WindowsType implements Dictionary {

    PVC("Plastikowe"),
    WOODEN("Drewniane"),
    ALUMINIUM("Aluminiowe"),
    NONE("Brak");

    private final String label;

    WindowsType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
