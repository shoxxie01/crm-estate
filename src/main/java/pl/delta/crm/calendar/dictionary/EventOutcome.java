package pl.delta.crm.calendar.dictionary;

import pl.delta.crm.property.dictionary.Dictionary;

/**
 * Rezultat odbytego zdarzenia — wypełniany dopiero przy statusie
 * {@code COMPLETED} (pilnuje tego serwis i CHECK w migracji V10).
 *
 * <p>To jedyne pole kalendarza, które niesie informację zwrotną o ofercie:
 * seria prezentacji zamknięta jako {@link #PRICE_TOO_HIGH} jest argumentem
 * w rozmowie z właścicielem o obniżce, a nie tylko historią spotkań.
 */
public enum EventOutcome implements Dictionary {

    INTERESTED("Zainteresowany"),
    CONSIDERING("Do namysłu"),
    OFFER_MADE("Złożył ofertę"),
    PRICE_TOO_HIGH("Cena za wysoka"),
    NOT_INTERESTED("Brak zainteresowania"),
    CONTRACT_SIGNED("Umowa podpisana"),
    INCONCLUSIVE("Bez rozstrzygnięcia");

    private final String label;

    EventOutcome(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
