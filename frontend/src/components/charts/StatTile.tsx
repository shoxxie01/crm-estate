import { ArrowDownRight, ArrowUpRight, Minus } from "lucide-react";
import type { KpiDatum } from "../../data/dashboard";
import { formatCompactPLN, formatDelta, formatNumber } from "../../lib/format";
import { Sparkline } from "./Sparkline";
import { cn } from "../../lib/cn";

export function StatTile({ kpi }: { kpi: KpiDatum }) {
  const value =
    kpi.format === "currencyCompact"
      ? formatCompactPLN(kpi.value)
      : formatNumber(kpi.value);

  const isFlat = kpi.delta === 0;
  const isGood = kpi.higherIsBetter ? kpi.delta > 0 : kpi.delta < 0;
  const DeltaIcon = isFlat
    ? Minus
    : kpi.delta > 0
      ? ArrowUpRight
      : ArrowDownRight;

  return (
    <div className="card p-4">
      <p className="text-[12px] text-ink-secondary">{kpi.label}</p>

      <div className="mt-2 flex items-end justify-between gap-3">
        {/* Duża liczba: cyfry proporcjonalne, nie tabularne */}
        <p className="text-[26px] leading-none font-semibold tracking-tight text-ink">
          {value}
        </p>
        <Sparkline points={kpi.trend} />
      </div>

      <div className="mt-2.5 flex items-center gap-1.5 text-[12px]">
        <span
          className={cn(
            "inline-flex items-center gap-0.5 font-medium",
            isFlat
              ? "text-ink-muted"
              : isGood
                ? "text-delta-up"
                : "text-delta-down",
          )}
        >
          <DeltaIcon className="size-3.5" strokeWidth={2} aria-hidden="true" />
          {formatDelta(kpi.delta, kpi.deltaUnit)}
        </span>
        <span className="truncate text-ink-muted">{kpi.deltaLabel}</span>
      </div>
    </div>
  );
}
