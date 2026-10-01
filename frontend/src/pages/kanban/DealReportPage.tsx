import { useEffect, useMemo, useState } from "react";
import { Info } from "lucide-react";
import { ApiError } from "../../api/client";
import {
  fetchDealDictionaries,
  fetchDealReport,
  type DealDictionaries,
  type DealReport,
} from "../../api/deals";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { cn } from "../../lib/cn";
import { formatCompactPLN, formatCurrency, formatNumber } from "../../lib/format";
import { KanbanTabs } from "./KanbanTabs";

/** Tyle zamkniętych transakcji serwer wymaga, żeby pokazać szansę wygranej (MIN_SAMPLE). */
const MIN_SAMPLE = 5;

/** Poniżej tylu transakcji w okresie procenty są bardziej przypadkiem niż statystyką. */
const SMALL_COHORT = 10;

const PERIODS = [
  { value: 30, label: "30 dni" },
  { value: 90, label: "Kwartał" },
  { value: 365, label: "Rok" },
  { value: 0, label: "Od początku" },
];

/** Jeden odcień dla wielkości. Słupki lejka i powodów to „ile", nie „kto". */
const BAR = "var(--color-series-1)";

const percent = (value: number | null | undefined) =>
  value == null ? "-" : `${Math.round(value * 100)}%`;

const days = (value: number | null | undefined) =>
  value == null ? "-" : `${formatNumber(value)} ${value === 1 ? "dzień" : "dni"}`;

/**
 * Raport lejka. Co biuro wyczytuje z historii etapów: gdzie odpadają
 * transakcje, ile trwa każdy etap, dlaczego przegrywamy i czego spodziewać się
 * po kartach otwartych dziś.
 */
export function DealReportPage() {
  const [period, setPeriod] = useState(90);
  const [mine, setMine] = useState(false);
  const [report, setReport] = useState<DealReport | null>(null);
  const [dictionaries, setDictionaries] = useState<DealDictionaries | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDealDictionaries().then(setDictionaries).catch(() => undefined);
  }, []);

  useEffect(() => {
    const to = new Date();
    // „Od początku" = od dnia, przed którym biuro na pewno nie miało transakcji.
    const from = period === 0 ? new Date(2000, 0, 1) : new Date(to.getTime() - period * 86_400_000);
    setLoading(true);
    fetchDealReport({ from, to, mine })
      .then((result) => {
        setReport(result);
        setError(null);
      })
      .catch((cause) =>
        setError(cause instanceof ApiError ? cause.message : "Nie udało się wczytać raportu."),
      )
      .finally(() => setLoading(false));
  }, [period, mine]);

  const labels = useMemo(() => {
    const map = new Map<string, string>();
    for (const entry of [...(dictionaries?.stage ?? []), ...(dictionaries?.lostReason ?? [])]) {
      map.set(entry.value, entry.label);
    }
    return map;
  }, [dictionaries]);
  const label = (value: string) => labels.get(value) ?? value;

  return (
    <div className="flex flex-col gap-4">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-col gap-2">
          <h1 className="text-base font-semibold tracking-tight text-ink">Kanban</h1>
          <KanbanTabs />
        </div>

        {/* Filtry w jednym rzędzie nad wykresami. */}
        <div className="flex flex-wrap items-end gap-2">
          <div className="flex rounded-md border border-line bg-surface p-0.5" role="group" aria-label="Okres">
            {PERIODS.map((option) => (
              <button
                key={option.value}
                onClick={() => setPeriod(option.value)}
                aria-pressed={period === option.value}
                className={cn(
                  "rounded px-2.5 py-1 text-[12px] transition-colors",
                  period === option.value
                    ? "bg-subtle font-medium text-ink"
                    : "text-ink-secondary hover:text-ink",
                )}
              >
                {option.label}
              </button>
            ))}
          </div>
          <Button
            variant={mine ? "primary" : "secondary"}
            onClick={() => setMine((current) => !current)}
            aria-pressed={mine}
          >
            Tylko moje
          </Button>
        </div>
      </header>

      {error && (
        <p className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {error}
        </p>
      )}

      {!report ? (
        <p className="text-[13px] text-ink-muted">{loading ? "Wczytywanie…" : ""}</p>
      ) : (
        <div className={cn("flex flex-col gap-4", loading && "opacity-60 transition-opacity")}>
          <p className="text-[12px] text-ink-muted">
            Transakcje założone od {new Date(report.from).toLocaleDateString("pl-PL")} do{" "}
            {new Date(report.to).toLocaleDateString("pl-PL")}. Prognoza dotyczy kart otwartych dziś.
          </p>

          {report.totals.created > 0 && report.totals.created < SMALL_COHORT && (
            <p className="flex items-start gap-2 rounded-md border border-line bg-subtle px-3 py-2 text-[12px] text-ink-secondary">
              <Info className="mt-0.5 size-3.5 shrink-0 text-ink-muted" strokeWidth={2} />
              W tym okresie jest tylko {report.totals.created}{" "}
              {report.totals.created === 1 ? "transakcja" : "transakcji"}. Procenty mogą być
              przypadkowe. Raport nabiera sensu po kilku tygodniach pracy na tablicy.
            </p>
          )}

          <Kpis report={report} />

          {report.totals.created === 0 ? (
            <Card>
              <p className="text-[13px] text-ink-muted">
                W tym okresie nie założono żadnej transakcji. Zmień okres albo wyłącz „Tylko moje”.
              </p>
            </Card>
          ) : (
            <>
              <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
                <Funnel report={report} label={label} />
                <LostReasons report={report} label={label} />
              </div>
              <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
                <StageTimes report={report} label={label} />
                <Agents report={report} />
              </div>
            </>
          )}

          <Forecast report={report} label={label} />
        </div>
      )}
    </div>
  );
}

// --- sekcje ----------------------------------------------------------------

function Kpis({ report }: { report: DealReport }) {
  const { totals } = report;
  const tiles = [
    {
      label: "Założone transakcje",
      value: formatNumber(totals.created),
      note: `${formatNumber(totals.open)} wciąż otwartych`,
    },
    {
      label: "Skuteczność",
      value: percent(totals.winRate),
      note:
        totals.winRate == null
          ? "Brak zamkniętych transakcji"
          : `${totals.won} wygranych · ${totals.lost} przegranych`,
    },
    {
      label: "Wartość wygranych",
      value: formatCompactPLN(totals.wonValue),
      note: `Prowizje ${formatCompactPLN(totals.wonCommission)}`,
    },
    {
      label: "Średni czas do wygranej",
      value: days(totals.avgCycleDays),
      note: "Od założenia karty do zamknięcia",
    },
  ];

  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
      {tiles.map((tile) => (
        <div key={tile.label} className="card p-4">
          <p className="text-[12px] text-ink-secondary">{tile.label}</p>
          <p className="mt-2 text-[26px] leading-none font-semibold tracking-tight text-ink">
            {tile.value}
          </p>
          <p className="mt-2.5 truncate text-[12px] text-ink-muted">{tile.note}</p>
        </div>
      ))}
    </div>
  );
}

/**
 * Lejek: ile transakcji doszło do każdego etapu. Słupek to liczba, obok
 * przejście z poprzedniego szczebla. Najniższe przejście to miejsce, gdzie
 * biuro traci najwięcej, więc dostaje wyróżnienie tekstem, nie kolorem.
 */
function Funnel({ report, label }: { report: DealReport; label: (value: string) => string }) {
  const [hovered, setHovered] = useState<string | null>(null);
  const max = Math.max(1, ...report.funnel.map((step) => step.reached));
  const weakest = report.funnel
    .filter((step) => step.conversion != null && step.stage !== "WON")
    .reduce<(typeof report.funnel)[number] | null>(
      (lowest, step) => (lowest == null || step.conversion! < lowest.conversion! ? step : lowest),
      null,
    );

  return (
    <Card
      title="Lejek"
      description="Ile transakcji doszło co najmniej do danego etapu. Także przeskakując kolumny."
    >
      <ul className="flex flex-col gap-3">
        {report.funnel.map((step) => {
          const isHovered = hovered === step.stage;
          return (
            <li
              key={step.stage}
              onMouseEnter={() => setHovered(step.stage)}
              onMouseLeave={() => setHovered(null)}
            >
              <div className="mb-1 flex items-baseline justify-between gap-2 text-[12px]">
                <span className={cn("truncate", isHovered ? "text-ink" : "text-ink-secondary")}>
                  {label(step.stage)}
                </span>
                <span className="tabular flex shrink-0 items-baseline gap-2">
                  {step.conversion != null && (
                    <span
                      className={cn(
                        weakest?.stage === step.stage ? "font-medium text-ink" : "text-ink-muted",
                      )}
                      title="Przejście z poprzedniego etapu"
                    >
                      {percent(step.conversion)} z poprzedniego
                      {weakest?.stage === step.stage && " · najsłabsze przejście"}
                    </span>
                  )}
                  <span className={cn("font-medium", isHovered ? "text-ink" : "text-ink-secondary")}>
                    {formatNumber(step.reached)}
                  </span>
                </span>
              </div>
              <div className="h-2.5 w-full">
                <div
                  className="h-full rounded-r-[4px] transition-[filter]"
                  style={{
                    width: `${(step.reached / max) * 100}%`,
                    minWidth: step.reached > 0 ? 4 : 0,
                    backgroundColor: BAR,
                    filter: isHovered ? "brightness(1.08)" : undefined,
                  }}
                />
              </div>
              {isHovered && step.lostHere > 0 && (
                <p className="mt-1 text-[12px] text-ink-muted">
                  Na tym etapie przegrano {step.lostHere}{" "}
                  {step.lostHere === 1 ? "transakcję" : "transakcji"}.
                </p>
              )}
            </li>
          );
        })}
      </ul>
    </Card>
  );
}

function LostReasons({ report, label }: { report: DealReport; label: (value: string) => string }) {
  const total = report.lostReasons.reduce((sum, row) => sum + row.count, 0);
  const max = Math.max(1, ...report.lostReasons.map((row) => row.count));
  const lostAt = report.funnel.filter((step) => step.lostHere > 0);

  return (
    <Card title="Dlaczego przegrywamy" description={`${total} przegranych w okresie`}>
      {total === 0 ? (
        <p className="text-[13px] text-ink-muted">Brak przegranych w tym okresie.</p>
      ) : (
        <div className="flex flex-col gap-4">
          <ul className="flex flex-col gap-3">
            {report.lostReasons.map((row) => (
              <li key={row.reason}>
                <div className="mb-1 flex items-baseline justify-between gap-2 text-[12px]">
                  <span className="truncate text-ink-secondary">{label(row.reason)}</span>
                  <span className="tabular shrink-0 text-ink-secondary">
                    {formatNumber(row.count)}
                    <span className="ml-1.5 text-ink-muted">{percent(row.count / total)}</span>
                  </span>
                </div>
                <div className="h-2.5 w-full">
                  <div
                    className="h-full rounded-r-[4px]"
                    style={{ width: `${(row.count / max) * 100}%`, backgroundColor: BAR }}
                  />
                </div>
              </li>
            ))}
          </ul>
          <p className="border-t border-line pt-3 text-[12px] text-ink-secondary">
            Odpadają na etapie:{" "}
            {lostAt.map((step, index) => (
              <span key={step.stage}>
                {index > 0 && ", "}
                <span className="text-ink">{label(step.stage)}</span>{" "}
                <span className="tabular">({step.lostHere})</span>
              </span>
            ))}
          </p>
        </div>
      )}
    </Card>
  );
}

function StageTimes({ report, label }: { report: DealReport; label: (value: string) => string }) {
  const max = Math.max(1, ...report.stageTimes.map((row) => row.medianDays ?? 0));

  return (
    <Card
      title="Czas w etapie"
      description="Z zakończonych pobytów w etapie."
      bodyClassName="p-0"
    >
      <table className="w-full text-[12px]">
        <thead>
          <tr className="border-b border-line text-left text-ink-muted">
            <th className="px-4 py-2 font-medium">Etap</th>
            <th className="w-[35%] px-2 py-2 font-medium">Mediana</th>
            <th className="px-2 py-2 text-right font-medium">Średnia</th>
            <th className="px-4 py-2 text-right font-medium">Próba</th>
          </tr>
        </thead>
        <tbody>
          {report.stageTimes.map((row) => (
            <tr key={row.stage} className="border-b border-line last:border-0">
              <td className="px-4 py-2 text-ink">{label(row.stage)}</td>
              <td className="px-2 py-2">
                {row.medianDays == null ? (
                  <span className="text-ink-muted">-</span>
                ) : (
                  <span className="flex items-center gap-2">
                    <span className="h-1.5 flex-1">
                      <span
                        className="block h-full rounded-r-[4px]"
                        style={{
                          width: `${(row.medianDays / max) * 100}%`,
                          minWidth: 4,
                          backgroundColor: BAR,
                        }}
                      />
                    </span>
                    <span className="w-14 shrink-0 text-right text-ink">{days(row.medianDays)}</span>
                  </span>
                )}
              </td>
              <td className="px-2 py-2 text-right text-ink-secondary">{days(row.avgDays)}</td>
              <td className="px-4 py-2 text-right text-ink-muted">{row.samples}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}

function Agents({ report }: { report: DealReport }) {
  return (
    <Card title="Agenci" description="Transakcje założone w okresie." bodyClassName="p-0">
      <table className="w-full text-[12px]">
        <thead>
          <tr className="border-b border-line text-left text-ink-muted">
            <th className="px-4 py-2 font-medium">Agent</th>
            <th className="px-2 py-2 text-right font-medium">Założone</th>
            <th className="px-2 py-2 text-right font-medium">Wygrane</th>
            <th className="px-2 py-2 text-right font-medium">Przegrane</th>
            <th className="px-2 py-2 text-right font-medium">Skuteczność</th>
            <th className="px-4 py-2 text-right font-medium">Wartość wygranych</th>
          </tr>
        </thead>
        <tbody>
          {report.agents.map((row) => (
            <tr key={row.agentId} className="border-b border-line last:border-0">
              <td className="px-4 py-2 text-ink">{row.agentName}</td>
              <td className="px-2 py-2 text-right text-ink-secondary">{row.created}</td>
              <td className="px-2 py-2 text-right text-ink-secondary">{row.won}</td>
              <td className="px-2 py-2 text-right text-ink-secondary">{row.lost}</td>
              <td className="px-2 py-2 text-right text-ink">{percent(row.winRate)}</td>
              <td className="px-4 py-2 text-right text-ink">{formatCompactPLN(row.wonValue)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}

/**
 * Prognoza: wartość otwartych kart × historyczna szansa wygranej z etapu.
 * Etap z za małą próbą nie dostaje procentu. Zamiast zgadywać, mówimy wprost,
 * ilu zamkniętych transakcji brakuje.
 */
function Forecast({ report, label }: { report: DealReport; label: (value: string) => string }) {
  const { forecast } = report;
  const unknown = forecast.stages.some((row) => row.openCount > 0 && row.winRate == null);

  return (
    <Card
      title="Prognoza dla otwartych kart"
      description="Wartość kart w etapie × szansa wygranej liczona z zamkniętych transakcji biura."
      action={
        <p className="text-right text-[12px] text-ink-secondary">
          Spodziewana sprzedaż{" "}
          <span className="block text-[18px] font-semibold tracking-tight text-ink">
            {formatCompactPLN(forecast.expectedValue)}
          </span>
          z {formatCompactPLN(forecast.openValue)} w lejku
        </p>
      }
      bodyClassName="p-0"
    >
      <div className="overflow-x-auto">
        <table className="w-full min-w-[560px] text-[12px]">
          <thead>
            <tr className="border-b border-line text-left text-ink-muted">
              <th className="px-4 py-2 font-medium">Etap</th>
              <th className="px-2 py-2 text-right font-medium">Karty</th>
              <th className="px-2 py-2 text-right font-medium">Wartość</th>
              <th className="px-2 py-2 text-right font-medium">Szansa wygranej</th>
              <th className="px-4 py-2 text-right font-medium">Spodziewane</th>
            </tr>
          </thead>
          <tbody>
            {forecast.stages.map((row) => (
              <tr key={row.stage} className="border-b border-line last:border-0">
                <td className="px-4 py-2 text-ink">{label(row.stage)}</td>
                <td className="px-2 py-2 text-right text-ink-secondary">{row.openCount}</td>
                <td className="px-2 py-2 text-right text-ink-secondary">
                  {row.openValue > 0 ? formatCurrency(row.openValue) : "-"}
                </td>
                <td className="px-2 py-2 text-right">
                  {row.winRate != null ? (
                    <span className="text-ink">{percent(row.winRate)}</span>
                  ) : (
                    <span className="text-ink-muted" title="Zamknięte transakcje, które doszły do tego etapu">
                      za mało danych ({row.sample}/{MIN_SAMPLE})
                    </span>
                  )}
                </td>
                <td className="px-4 py-2 text-right text-ink">
                  {row.expectedValue != null ? formatCurrency(row.expectedValue) : "-"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {unknown && (
        <p className="border-t border-line px-4 py-2.5 text-[12px] text-ink-muted">
          Etapy bez szansy wygranej nie wchodzą do spodziewanej sprzedaży. Potrzeba co
          najmniej {MIN_SAMPLE} zamkniętych transakcji, które do nich doszły.
        </p>
      )}
    </Card>
  );
}
