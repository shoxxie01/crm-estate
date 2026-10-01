package pl.delta.crm.deal.dictionary;

import pl.delta.crm.property.dictionary.Dictionary;

/**
 * Etap transakcji. Kolumna tablicy Kanban. Kolejność stałych jest kolejnością
 * kolumn na tablicy, więc nowy etap dopisuje się w miejscu, w którym ma stać.
 *
 * <p>Etapy nie są wymuszaną ścieżką: agent może przeskoczyć z leada od razu do
 * umowy pośrednictwa albo cofnąć kartę o krok, gdy kupujący się wycofa.
 * Twarda maszyna stanów nauczyłaby go tylko przeklikiwać kolumny po kolei.
 */
public enum DealStage implements Dictionary {

    LEAD("Nowy lead"),
    VALUATION("Wycena / spotkanie"),
    MANDATE("Umowa pośrednictwa"),
    MARKETING("Aktywna sprzedaż"),
    NEGOTIATION("Negocjacje"),
    RESERVATION("Rezerwacja / przedwstępna"),
    CLOSING("Finalizacja"),
    WON("Wygrana"),
    LOST("Przegrana");

    private final String label;

    DealStage(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    /** Wygrana i przegrana zamykają transakcję. Nie liczą się do wartości lejka. */
    public boolean isClosed() {
        return this == WON || this == LOST;
    }
}
