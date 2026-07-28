import { useState } from "react";
import type { LeadSource } from "../../data/dashboard";
import { formatNumber } from "../../lib/format";
import { cn } from "../../lib/cn";

/** Ramp sekwencyjny (jeden odcień, więcej = ciemniej) — zwalidowany na tle #ffffff. */
const RAMP = [
  "var(--color-seq-5)",
  "var(--color-seq-4)",
  "var(--color-seq-3)",
  "var(--color-seq-2)",
  "var(--color-seq-1)",
];

export function RankedBarChart({ data }: { data: LeadSource[] }) {
  const [hovered, setHovered] = useState<number | null>(null);

  const max = Math.max(...data.map((d) => d.value));
  const total = data.reduce((sum, d) => sum + d.value, 0);

  return (
    <ul className="flex flex-col gap-3">
      {data.map((row, index) => {
        const share = Math.round((row.value / total) * 100);
        const isHovered = hovered === index;

        return (
          <li
            key={row.label}
            onMouseEnter={() => setHovered(index)}
            onMouseLeave={() => setHovered(null)}
            className="relative"
          >
            <div className="mb-1 flex items-baseline justify-between gap-2">
              <span className="truncate text-[12px] text-ink-secondary">
                {row.label}
              </span>
              <span
                className={cn(
                  "tabular shrink-0 text-[12px] transition-colors",
                  isHovered ? "text-ink" : "text-ink-secondary",
                )}
              >
                {formatNumber(row.value)}
                <span className="ml-1.5 text-ink-muted">{share}%</span>
              </span>
            </div>

            <div className="h-2.5 w-full">
              <div
                className="h-full rounded-r-[4px] transition-[filter]"
                style={{
                  width: `${(row.value / max) * 100}%`,
                  backgroundColor: RAMP[Math.min(index, RAMP.length - 1)],
                  filter: isHovered ? "brightness(1.08)" : undefined,
                }}
              />
            </div>
          </li>
        );
      })}
    </ul>
  );
}
