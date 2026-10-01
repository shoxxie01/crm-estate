package pl.delta.crm.property.dictionary;

/**
 * Stan wykończenia (Otodom: ConstructionStatus).
 *
 * <p>Otodom trzyma dla mieszkania trzy wartości, a dla domu i hali pięć.
 * Z dwoma stanami surowymi. Zbiór jest sumą; {@link #DEVELOPER_STANDARD} dochodzi
 * od nas, bo to najczęściej używane określenie na rynku pierwotnym.
 */
public enum ConstructionStatus implements Dictionary {

    READY_TO_MOVE_IN("Do zamieszkania"),
    DEVELOPER_STANDARD("Stan deweloperski"),
    NEEDS_FINISHING("Do wykończenia"),
    NEEDS_RENOVATION("Do remontu"),
    SHELL_CLOSED("Stan surowy zamknięty"),
    SHELL_OPEN("Stan surowy otwarty");

    private final String label;

    ConstructionStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
