import { AlertTriangle, Check, HelpCircle, Star } from "lucide-react";
import type { MatchCriterion } from "../api/matching";
import { cn } from "../lib/cn";

/**
 * Wyjaśnienie dopasowania: co się zgadza, co jest „prawie", czego oferta nie
 * mówi. Spełnione kryteria są krótkie (sama nazwa), a te wymagające uwagi
 * niosą uzasadnienie — to z nimi agent idzie do telefonu.
 */
export function MatchCriteria({ criteria }: { criteria: MatchCriterion[] }) {
  // Rodzaj zgadza się zawsze (inaczej pozycji nie byłoby na liście) — nie zaśmiecamy nim.
  const shown = criteria.filter((c) => c.criterion !== "PROPERTY_TYPE");
  if (shown.length === 0) {
    return (
      <p className="text-[12px] text-ink-muted">
        Klient nie podał kryteriów poza rodzajem nieruchomości.
      </p>
    );
  }

  return (
    <ul className="flex flex-wrap gap-1.5">
      {shown.map((c) => {
        const preferred = c.criterion === "PREFERRED_FEATURES";
        const Icon = preferred
          ? Star
          : c.verdict === "MET"
            ? Check
            : c.verdict === "NEAR"
              ? AlertTriangle
              : HelpCircle;
        return (
          <li
            key={c.criterion}
            title={
              c.verdict === "NEAR"
                ? "Blisko, ale poza zakresem"
                : c.verdict === "UNKNOWN"
                  ? "Oferta nie ma danych — do dopytania"
                  : undefined
            }
            className={cn(
              "inline-flex items-center gap-1 rounded border px-1.5 py-0.5 text-[11px]",
              preferred
                ? "border-dashed border-line-strong text-ink-secondary"
                : c.verdict === "MET"
                  ? "border-good/30 bg-good/8 text-[#0a7a0a]"
                  : c.verdict === "NEAR"
                    ? "border-warning/40 bg-warning/12 text-[#8a5c00]"
                    : "border-line bg-subtle text-ink-secondary",
            )}
          >
            <Icon className="size-3 shrink-0" strokeWidth={2.25} />
            <span className="font-medium">{c.label}</span>
            {c.note && <span className="tabular-nums">· {c.note}</span>}
          </li>
        );
      })}
    </ul>
  );
}
