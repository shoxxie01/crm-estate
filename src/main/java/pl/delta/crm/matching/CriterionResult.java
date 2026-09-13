package pl.delta.crm.matching;

/**
 * Jedno kryterium w wyjaśnieniu dopasowania.
 *
 * @param note krótkie uzasadnienie dla agenta („o 6% ponad budżet", „brak: Winda");
 *             przy spełnionym kryterium zwykle puste
 */
public record CriterionResult(MatchCriterion criterion, String label, MatchVerdict verdict, String note) {

    static CriterionResult of(MatchCriterion criterion, MatchVerdict verdict, String note) {
        return new CriterionResult(criterion, criterion.label(), verdict, note);
    }

    static CriterionResult met(MatchCriterion criterion) {
        return of(criterion, MatchVerdict.MET, null);
    }
}
