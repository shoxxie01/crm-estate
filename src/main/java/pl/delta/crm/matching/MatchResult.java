package pl.delta.crm.matching;

import java.util.List;

/**
 * Wynik porównania poszukiwania z ofertą.
 *
 * @param preferredHits ile cech „mile widzianych" oferta ma — służy do kolejności
 */
public record MatchResult(List<CriterionResult> criteria, int preferredHits) {

    public boolean matches() {
        return criteria.stream().noneMatch(c -> c.verdict() == MatchVerdict.MISSED);
    }

    /** Ile kryteriów wymaga uwagi agenta (prawie / brak danych). Mile widziane się nie liczą. */
    public long warnings() {
        return criteria.stream()
                .filter(c -> c.criterion() != MatchCriterion.PREFERRED_FEATURES)
                .filter(c -> c.verdict() == MatchVerdict.NEAR || c.verdict() == MatchVerdict.UNKNOWN)
                .count();
    }
}
