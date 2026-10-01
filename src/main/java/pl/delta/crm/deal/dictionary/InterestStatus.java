package pl.delta.crm.deal.dictionary;

import pl.delta.crm.property.dictionary.Dictionary;

/**
 * Gdzie jest jeden zainteresowany w obrębie transakcji. Mini-lejek popytu
 * na karcie. Kolejność stałych to kolejność na liście w panelu.
 */
public enum InterestStatus implements Dictionary {

    NEW("Zgłosił(a) się"),
    VIEWED("Oglądał(a)"),
    CONSIDERING("Do namysłu"),
    OFFER("Złożył(a) ofertę"),
    ACCEPTED("Oferta przyjęta"),
    DROPPED("Odpadł(a)");

    private final String label;

    InterestStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    /** Zainteresowany, który wciąż jest w grze. Liczony na karcie tablicy. */
    public boolean isActive() {
        return this != DROPPED;
    }

    /** Ma na stole konkretną kwotę. */
    public boolean hasOffer() {
        return this == OFFER || this == ACCEPTED;
    }
}
