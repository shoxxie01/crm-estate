package pl.delta.crm.calendar.dictionary;

import pl.delta.crm.property.dictionary.Dictionary;

/**
 * Stan terminu. Rozróżnienie {@link #CANCELLED} od {@link #NO_SHOW} nie jest
 * kosmetyczne: pierwsze to termin, który nie odbył się z ustaleniem obu stron,
 * drugie to strata czasu agenta — i tylko drugie warto zliczać przy ocenie
 * jakości leadów.
 */
public enum EventStatus implements Dictionary {

    PLANNED("Planowane"),

    /** Druga strona potwierdziła — termin jest pewny. */
    CONFIRMED("Potwierdzone"),

    COMPLETED("Odbyło się"),

    CANCELLED("Odwołane"),

    /** Klient się nie stawił i nie uprzedził. */
    NO_SHOW("Nieobecność");

    private final String label;

    EventStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    /** Terminy, które nadal zajmują agentowi czas — tylko te liczą się do kolizji. */
    public boolean blocksTime() {
        return this != CANCELLED;
    }
}
