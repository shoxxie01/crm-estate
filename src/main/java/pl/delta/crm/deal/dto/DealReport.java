package pl.delta.crm.deal.dto;

import pl.delta.crm.deal.dictionary.DealLostReason;
import pl.delta.crm.deal.dictionary.DealStage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Raport lejka sprzedaży. Wszystko, co da się wyczytać z historii etapów.
 *
 * <p>Okres {@code from}–{@code to} wyznacza <b>kohortę</b>: transakcje założone
 * w tym czasie. Lejek, powody przegranych i porównanie agentów liczą się na niej,
 * bo tylko wtedy procenty mają wspólny mianownik. „Z leadów z marca ile doszło
 * do umowy". Prognoza dotyczy za to kart otwartych <i>dziś</i>, niezależnie od
 * okresu, a szanse wygranej bierze z całej historii biura.
 *
 * <p>Wartości liczbowe, których nie da się rzetelnie policzyć (za mało danych),
 * są puste. Front pokazuje wtedy „za mało danych" zamiast przypadkowego procentu.
 */
public record DealReport(
        Instant from,
        Instant to,
        Totals totals,
        List<FunnelStep> funnel,
        List<StageTime> stageTimes,
        List<LostReasonCount> lostReasons,
        List<AgentRow> agents,
        Forecast forecast
) {

    /**
     * @param winRate      wygrane / (wygrane + przegrane) w kohorcie; pusty bez zamkniętych
     * @param avgCycleDays średni czas od założenia do wygranej, dla wygranych z kohorty
     */
    public record Totals(int created, int open, int won, int lost,
                         Double winRate, BigDecimal wonValue, BigDecimal wonCommission,
                         Double avgCycleDays) {
    }

    /**
     * Jeden szczebel lejka.
     *
     * @param reached    ile transakcji z kohorty doszło co najmniej do tego etapu
     * @param conversion udział względem poprzedniego szczebla; pusty dla pierwszego
     * @param lostHere   ile przegranych odpadło właśnie na tym etapie
     */
    public record FunnelStep(DealStage stage, int reached, Double conversion, int lostHere) {
    }

    /**
     * Czas spędzony w etapie. Z zakończonych pobytów (karta weszła i wyszła).
     * Mediana obok średniej, bo jedna transakcja zapomniana na pół roku
     * potrafi zawyżyć średnią dla całego biura.
     */
    public record StageTime(DealStage stage, int samples, Double medianDays, Double avgDays) {
    }

    public record LostReasonCount(DealLostReason reason, int count) {
    }

    public record AgentRow(UUID agentId, String agentName, int created, int won, int lost,
                           Double winRate, BigDecimal wonValue) {
    }

    /**
     * @param expectedValue suma wartości ważonych szansą wygranej; liczona tylko
     *                      z etapów, dla których szansa jest znana
     */
    public record Forecast(BigDecimal openValue, BigDecimal expectedValue, List<ForecastStage> stages) {
    }

    /**
     * @param winRate szansa wygranej transakcji, która doszła do tego etapu.
     *                Z zamkniętych transakcji całego biura; pusta przy zbyt małej próbie
     * @param sample  na ilu zamkniętych transakcjach policzono szansę
     */
    public record ForecastStage(DealStage stage, int openCount, BigDecimal openValue,
                                Double winRate, int sample, BigDecimal expectedValue) {
    }
}
