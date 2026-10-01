package pl.delta.crm.deal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.delta.crm.deal.dictionary.DealLostReason;
import pl.delta.crm.deal.dictionary.DealStage;
import pl.delta.crm.deal.dto.DealReport;
import pl.delta.crm.error.BusinessValidationException;
import pl.delta.crm.user.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Raport lejka liczony z historii etapów ({@code deal_stage_changes}).
 *
 * <p>Liczenie odbywa się w Javie, a nie w SQL-u: biuro ma setki transakcji,
 * nie miliony, a reguły (najdalszy osiągnięty etap, przeskoczone kolumny,
 * pobyty w etapie z kolejnych wpisów historii) w SQL-u byłyby nieczytelne
 * i trudne do przetestowania.
 */
@Service
public class DealReportService {

    /**
     * Poniżej tylu zamkniętych transakcji szansa wygranej z danego etapu nie
     * jest pokazywana. Przy trzech transakcjach „67%" to przypadek, nie statystyka.
     */
    static final int MIN_SAMPLE = 5;

    private static final double DAY_SECONDS = 86_400d;

    /** Szczeble lejka: otwarte etapy po kolei i wygrana na końcu. */
    private static final List<DealStage> FUNNEL = Arrays.stream(DealStage.values())
            .filter(stage -> stage != DealStage.LOST)
            .toList();

    private static final List<DealStage> OPEN = Arrays.stream(DealStage.values())
            .filter(stage -> !stage.isClosed())
            .toList();

    private final DealRepository deals;
    private final DealStageChangeRepository history;

    public DealReportService(DealRepository deals, DealStageChangeRepository history) {
        this.deals = deals;
        this.history = history;
    }

    @Transactional(readOnly = true)
    public DealReport report(User viewer, Instant from, Instant to, UUID agentId) {
        if (!to.isAfter(from)) {
            throw new BusinessValidationException(
                    Map.of("to", "Koniec okresu musi być późniejszy niż początek."));
        }

        UUID agencyId = viewer.getAgency().getId();
        List<Deal> all = deals.findAllForReport(agencyId);

        Map<UUID, List<DealStageChange>> changes = new HashMap<>();
        for (DealStageChange change : history.findByAgency(agencyId)) {
            changes.computeIfAbsent(change.getDeal().getId(), key -> new ArrayList<>()).add(change);
        }

        List<Deal> scoped = all.stream()
                .filter(deal -> agentId == null || deal.getAgent().getId().equals(agentId))
                .toList();
        List<Deal> cohort = scoped.stream()
                .filter(deal -> !deal.getCreatedAt().isBefore(from) && deal.getCreatedAt().isBefore(to))
                .toList();

        return new DealReport(
                from,
                to,
                totals(cohort),
                funnel(cohort, changes),
                stageTimes(cohort, changes),
                lostReasons(cohort),
                agents(cohort),
                // Szanse wygranej z całego biura. Większa próba niż jeden agent.
                forecast(scoped, all, changes)
        );
    }

    // --- sekcje raportu ------------------------------------------------------

    private DealReport.Totals totals(List<Deal> cohort) {
        List<Deal> won = withStage(cohort, DealStage.WON);
        int lost = withStage(cohort, DealStage.LOST).size();

        Double avgCycle = won.isEmpty() ? null : round(won.stream()
                .mapToDouble(deal -> days(deal.getCreatedAt(), deal.getClosedAt()))
                .average()
                .orElse(0));

        return new DealReport.Totals(
                cohort.size(),
                (int) cohort.stream().filter(deal -> !deal.getStage().isClosed()).count(),
                won.size(),
                lost,
                ratio(won.size(), won.size() + lost),
                sum(won, Deal::getValue),
                sum(won, Deal::getCommission),
                avgCycle);
    }

    /**
     * Lejek „do najdalszego etapu": transakcja liczy się na każdym szczeblu,
     * do którego doszła albo który przeskoczyła. Dzięki temu karta przeniesiona
     * od razu z leada do umowy pośrednictwa nie robi dziury w „Wycenie",
     * a lejek zawsze się zwęża.
     */
    private List<DealReport.FunnelStep> funnel(List<Deal> cohort, Map<UUID, List<DealStageChange>> changes) {
        Map<DealStage, Integer> lostAt = new EnumMap<>(DealStage.class);
        for (Deal deal : withStage(cohort, DealStage.LOST)) {
            lostAt.merge(lostFrom(deal, changes.get(deal.getId())), 1, Integer::sum);
        }

        List<DealReport.FunnelStep> steps = new ArrayList<>();
        Integer previous = null;
        for (DealStage stage : FUNNEL) {
            int reached = (int) cohort.stream()
                    .filter(deal -> reachedStage(deal, changes.get(deal.getId()), stage))
                    .count();
            steps.add(new DealReport.FunnelStep(
                    stage,
                    reached,
                    previous == null ? null : ratio(reached, previous),
                    lostAt.getOrDefault(stage, 0)));
            previous = reached;
        }
        return steps;
    }

    /**
     * Pobyty w etapie z kolejnych wpisów historii: wejście do etapu → następna
     * zmiana. Pobyt jeszcze trwający nie wchodzi do statystyki. Inaczej świeże
     * karty zaniżałyby czasy, a zapomniane zawyżały bez końca.
     */
    private List<DealReport.StageTime> stageTimes(List<Deal> cohort, Map<UUID, List<DealStageChange>> changes) {
        Map<DealStage, List<Double>> stays = new EnumMap<>(DealStage.class);
        for (Deal deal : cohort) {
            List<DealStageChange> list = changes.getOrDefault(deal.getId(), List.of());
            for (int i = 0; i + 1 < list.size(); i++) {
                DealStage stage = list.get(i).getToStage();
                if (stage.isClosed()) {
                    continue; // czas „w przegranych" nic nie mówi o sprzedaży
                }
                stays.computeIfAbsent(stage, key -> new ArrayList<>())
                        .add(days(list.get(i).getChangedAt(), list.get(i + 1).getChangedAt()));
            }
        }

        return OPEN.stream()
                .map(stage -> {
                    List<Double> values = stays.getOrDefault(stage, List.of());
                    return new DealReport.StageTime(
                            stage,
                            values.size(),
                            values.isEmpty() ? null : round(median(values)),
                            values.isEmpty() ? null : round(values.stream().mapToDouble(Double::doubleValue)
                                    .average().orElse(0)));
                })
                .toList();
    }

    private List<DealReport.LostReasonCount> lostReasons(List<Deal> cohort) {
        Map<DealLostReason, Integer> counts = new EnumMap<>(DealLostReason.class);
        for (Deal deal : withStage(cohort, DealStage.LOST)) {
            counts.merge(deal.getLostReason(), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<DealLostReason, Integer>comparingByValue().reversed())
                .map(entry -> new DealReport.LostReasonCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<DealReport.AgentRow> agents(List<Deal> cohort) {
        Map<UUID, List<Deal>> byAgent = new LinkedHashMap<>();
        for (Deal deal : cohort) {
            byAgent.computeIfAbsent(deal.getAgent().getId(), key -> new ArrayList<>()).add(deal);
        }

        return byAgent.values().stream()
                .map(list -> {
                    User agent = list.getFirst().getAgent();
                    List<Deal> won = withStage(list, DealStage.WON);
                    int lost = withStage(list, DealStage.LOST).size();
                    return new DealReport.AgentRow(
                            agent.getId(),
                            agent.getFirstName() + " " + agent.getLastName(),
                            list.size(),
                            won.size(),
                            lost,
                            ratio(won.size(), won.size() + lost),
                            sum(won, Deal::getValue));
                })
                .sorted(Comparator.comparing(DealReport.AgentRow::wonValue).reversed()
                        .thenComparing(DealReport.AgentRow::created, Comparator.reverseOrder()))
                .toList();
    }

    /**
     * Prognoza: wartość otwartych kart ważona historyczną szansą wygranej
     * z etapu, w którym stoją. Szansa = ile zamkniętych transakcji, które doszły
     * do tego etapu, skończyło się wygraną.
     */
    private DealReport.Forecast forecast(List<Deal> scoped, List<Deal> all,
                                         Map<UUID, List<DealStageChange>> changes) {
        List<Deal> closed = all.stream().filter(deal -> deal.getStage().isClosed()).toList();

        BigDecimal openTotal = BigDecimal.ZERO;
        BigDecimal expectedTotal = BigDecimal.ZERO;
        List<DealReport.ForecastStage> stages = new ArrayList<>();

        for (DealStage stage : OPEN) {
            List<Deal> open = withStage(scoped, stage);
            BigDecimal openValue = sum(open, Deal::getValue);

            List<Deal> sample = closed.stream()
                    .filter(deal -> reachedStage(deal, changes.get(deal.getId()), stage))
                    .toList();
            Double winRate = sample.size() < MIN_SAMPLE ? null
                    : ratio(withStage(sample, DealStage.WON).size(), sample.size());
            BigDecimal expected = winRate == null ? null
                    : openValue.multiply(BigDecimal.valueOf(winRate)).setScale(2, RoundingMode.HALF_UP);

            openTotal = openTotal.add(openValue);
            if (expected != null) {
                expectedTotal = expectedTotal.add(expected);
            }
            stages.add(new DealReport.ForecastStage(stage, open.size(), openValue, winRate, sample.size(), expected));
        }

        return new DealReport.Forecast(openTotal, expectedTotal, stages);
    }

    // --- pomocnicze ----------------------------------------------------------

    /** Najdalszy otwarty etap, w jakim transakcja kiedykolwiek była. */
    static DealStage furthestOpen(Deal deal, List<DealStageChange> list) {
        DealStage furthest = deal.getStage().isClosed() ? DealStage.LEAD : deal.getStage();
        if (list != null) {
            for (DealStageChange change : list) {
                if (!change.getToStage().isClosed() && change.getToStage().ordinal() > furthest.ordinal()) {
                    furthest = change.getToStage();
                }
            }
        }
        return furthest;
    }

    /** Wygrana przeszła cały lejek; pozostałe liczą się do najdalszego otwartego etapu. */
    private static boolean reachedStage(Deal deal, List<DealStageChange> list, DealStage stage) {
        if (deal.getStage() == DealStage.WON) {
            return true;
        }
        return stage != DealStage.WON && furthestOpen(deal, list).ordinal() >= stage.ordinal();
    }

    /** Etap, z którego karta trafiła do przegranych. Ostatnie przejście do LOST. */
    private static DealStage lostFrom(Deal deal, List<DealStageChange> list) {
        if (list != null) {
            for (int i = list.size() - 1; i >= 0; i--) {
                DealStageChange change = list.get(i);
                if (change.getToStage() == DealStage.LOST && change.getFromStage() != null) {
                    return change.getFromStage();
                }
            }
        }
        return furthestOpen(deal, list);
    }

    private static List<Deal> withStage(List<Deal> list, DealStage stage) {
        return list.stream().filter(deal -> deal.getStage() == stage).toList();
    }

    private static BigDecimal sum(List<Deal> list, java.util.function.Function<Deal, BigDecimal> field) {
        return list.stream()
                .map(field)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Double ratio(int part, int whole) {
        return whole == 0 ? null : round((double) part / whole, 4);
    }

    private static double days(Instant from, Instant to) {
        return Duration.between(from, to).toSeconds() / DAY_SECONDS;
    }

    private static double median(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle) : (sorted.get(middle - 1) + sorted.get(middle)) / 2;
    }

    private static Double round(double value) {
        return round(value, 1);
    }

    private static Double round(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }
}
