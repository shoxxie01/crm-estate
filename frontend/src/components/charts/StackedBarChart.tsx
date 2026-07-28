import { useMemo, useState } from "react";
import type { TransactionMonth } from "../../data/dashboard";
import { cn } from "../../lib/cn";

const PLOT_HEIGHT = 200;

const SERIES = [
  { key: "sale", label: "Sprzedaż", color: "var(--color-series-1)" },
  { key: "rent", label: "Wynajem", color: "var(--color-series-2)" },
] as const;

/** Skala: zaokrąglij górę do wielokrotności 5, żeby ticki były okrągłe. */
function buildScale(maxValue: number) {
  const top = Math.ceil(maxValue / 5) * 5;
  const step = top / 4;
  return { top, ticks: [0, step, step * 2, step * 3, top] };
}

export function StackedBarChart({ data }: { data: TransactionMonth[] }) {
  const [hovered, setHovered] = useState<number | null>(null);
  const [view, setView] = useState<"chart" | "table">("chart");

  const { totals, scale, peakIndex } = useMemo(() => {
    const totals = data.map((d) => d.sale + d.rent);
    return {
      totals,
      scale: buildScale(Math.max(...totals)),
      peakIndex: totals.indexOf(Math.max(...totals)),
    };
  }, [data]);

  return (
    <figure className="m-0">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        {/* Legenda — obecna zawsze przy 2+ seriach */}
        <ul className="flex items-center gap-4">
          {SERIES.map((series) => (
            <li
              key={series.key}
              className="flex items-center gap-1.5 text-[12px] text-ink-secondary"
            >
              <span
                className="size-2.5 rounded-[2px]"
                style={{ backgroundColor: series.color }}
                aria-hidden="true"
              />
              {series.label}
            </li>
          ))}
        </ul>

        <div className="flex rounded-md border border-line p-0.5">
          {(["chart", "table"] as const).map((mode) => (
            <button
              key={mode}
              onClick={() => setView(mode)}
              aria-pressed={view === mode}
              className={cn(
                "rounded px-2 py-0.5 text-[12px] transition-colors",
                view === mode
                  ? "bg-subtle font-medium text-ink"
                  : "text-ink-muted hover:text-ink",
              )}
            >
              {mode === "chart" ? "Wykres" : "Tabela"}
            </button>
          ))}
        </div>
      </div>

      {view === "table" ? (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-[13px]">
            <thead>
              <tr className="border-b border-line text-[12px] text-ink-muted">
                <th className="py-1.5 pr-3 font-medium">Miesiąc</th>
                <th className="py-1.5 pr-3 text-right font-medium">Sprzedaż</th>
                <th className="py-1.5 pr-3 text-right font-medium">Wynajem</th>
                <th className="py-1.5 text-right font-medium">Razem</th>
              </tr>
            </thead>
            <tbody>
              {data.map((row, index) => (
                <tr key={row.month} className="border-b border-line last:border-0">
                  <td className="py-1.5 pr-3 text-ink-secondary">{row.month}</td>
                  <td className="py-1.5 pr-3 text-right text-ink">{row.sale}</td>
                  <td className="py-1.5 pr-3 text-right text-ink">{row.rent}</td>
                  <td className="py-1.5 text-right font-medium text-ink">
                    {totals[index]}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="pl-8">
          <div className="relative" style={{ height: PLOT_HEIGHT }}>
            {/* Siatka — hairline, recesywna */}
            {scale.ticks.map((tick) => (
              <div
                key={tick}
                aria-hidden="true"
                className={cn(
                  "absolute inset-x-0 h-px",
                  tick === 0 ? "bg-axis" : "bg-grid",
                )}
                style={{ bottom: `${(tick / scale.top) * 100}%` }}
              />
            ))}

            {/* Podpisy osi Y — cyfry tabularne */}
            {scale.ticks.map((tick) => (
              <span
                key={tick}
                aria-hidden="true"
                className="tabular absolute right-full mr-2 translate-y-1/2 text-[11px] text-ink-muted"
                style={{ bottom: `${(tick / scale.top) * 100}%` }}
              >
                {tick}
              </span>
            ))}

            {/* Kolumny */}
            <div className="absolute inset-0 flex items-end">
              {data.map((row, index) => {
                const total = totals[index];
                const totalPct = (total / scale.top) * 100;
                const rentPct = total === 0 ? 0 : (row.rent / total) * 100;
                const isHovered = hovered === index;

                return (
                  <div
                    key={row.month}
                    onMouseEnter={() => setHovered(index)}
                    onMouseLeave={() => setHovered(null)}
                    onFocus={() => setHovered(index)}
                    onBlur={() => setHovered(null)}
                    tabIndex={0}
                    role="button"
                    aria-label={`${row.month}: sprzedaż ${row.sale}, wynajem ${row.rent}, razem ${total}`}
                    className="relative flex h-full flex-1 cursor-default items-end justify-center rounded-sm focus:outline-none"
                  >
                    {/* Powiększony cel najazdu */}
                    <div
                      aria-hidden="true"
                      className={cn(
                        "absolute inset-x-[2px] inset-y-0 rounded-sm transition-colors",
                        isHovered && "bg-subtle",
                      )}
                    />

                    {/* Etykieta bezpośrednia — tylko na szczycie serii */}
                    {index === peakIndex && (
                      <span
                        className="tabular absolute z-10 text-[11px] font-medium text-ink-secondary"
                        style={{ bottom: `calc(${totalPct}% + 6px)` }}
                      >
                        {total}
                      </span>
                    )}

                    <div
                      className="relative flex w-full max-w-[24px] flex-col justify-end px-[3px]"
                      style={{ height: `${totalPct}%` }}
                    >
                      {/* Górny segment: 4px zaokrąglenie na końcu danych */}
                      <div
                        className="mb-[2px] w-full rounded-t-[4px]"
                        style={{
                          height: `${rentPct}%`,
                          backgroundColor: SERIES[1].color,
                        }}
                      />
                      {/* Dolny segment: kwadratowy przy linii bazowej */}
                      <div
                        className="w-full flex-1"
                        style={{ backgroundColor: SERIES[0].color }}
                      />
                    </div>

                    {isHovered && (
                      <div
                        className="card pointer-events-none absolute bottom-full left-1/2 z-20 mb-2 w-40 -translate-x-1/2 p-2.5 shadow-[0_4px_16px_rgba(26,29,33,0.10)]"
                        role="tooltip"
                      >
                        <p className="mb-1.5 text-[12px] font-medium text-ink">
                          {row.month} · razem {total}
                        </p>
                        {SERIES.map((series) => (
                          <p
                            key={series.key}
                            className="flex items-center gap-1.5 text-[12px] text-ink-secondary"
                          >
                            <span
                              className="size-2 shrink-0 rounded-[2px]"
                              style={{ backgroundColor: series.color }}
                            />
                            {series.label}
                            <span className="tabular ml-auto text-ink">
                              {row[series.key]}
                            </span>
                          </p>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Oś X */}
          <div className="mt-2 flex">
            {data.map((row) => (
              <span
                key={row.month}
                className="flex-1 text-center text-[11px] text-ink-muted"
              >
                {row.month}
              </span>
            ))}
          </div>
        </div>
      )}
    </figure>
  );
}
