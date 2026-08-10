package pl.delta.crm.property.dictionary;

/**
 * Rynek pierwotny / wtórny (Otodom: MarketType). Pole obowiązkowe przy imporcie
 * dla każdego typu obiektu — dlatego jest {@code NOT NULL} także u nas.
 */
public enum MarketType implements Dictionary {

    PIERWOTNY("Pierwotny"),
    WTORNY("Wtórny");

    private final String label;

    MarketType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
