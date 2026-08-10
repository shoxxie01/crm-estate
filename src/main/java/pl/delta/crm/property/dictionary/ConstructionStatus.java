package pl.delta.crm.property.dictionary;

/**
 * Stan wykończenia (Otodom: ConstructionStatus).
 *
 * <p>Otodom trzyma dla mieszkania trzy wartości, a dla domu i hali pięć —
 * z dwoma stanami surowymi. Zbiór jest sumą; {@link #DEWELOPERSKI} dochodzi
 * od nas, bo to najczęściej używane określenie na rynku pierwotnym.
 */
public enum ConstructionStatus implements Dictionary {

    DO_ZAMIESZKANIA("Do zamieszkania"),
    DEWELOPERSKI("Stan deweloperski"),
    DO_WYKONCZENIA("Do wykończenia"),
    DO_REMONTU("Do remontu"),
    STAN_SUROWY_ZAMKNIETY("Stan surowy zamknięty"),
    STAN_SUROWY_OTWARTY("Stan surowy otwarty");

    private final String label;

    ConstructionStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
