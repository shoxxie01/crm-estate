import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ChevronDown } from "lucide-react";
import { fetchRequirementMatches, type PropertyMatch } from "../../api/matching";
import { Badge } from "../../components/ui/Badge";
import { MatchCriteria } from "../../components/MatchCriteria";
import { cn } from "../../lib/cn";
import { formatCurrency, formatNumber } from "../../lib/format";

/** Oferty niegotowe do pokazania na portalach — agent musi wiedzieć, zanim zadzwoni. */
const STATUS_NOTE: Record<string, { label: string; tone: "neutral" | "warning" }> = {
  DRAFT: { label: "Robocza", tone: "neutral" },
  RESERVED: { label: "Zarezerwowana", tone: "warning" },
};

/**
 * „Co mamy dla tego klienta" — oferty biura pasujące do jednego poszukiwania.
 * `version` (np. updatedAt poszukiwania) wymusza ponowne liczenie po edycji.
 */
export function RequirementMatches({
  clientId,
  requirementId,
  version,
}: {
  clientId: string;
  requirementId: string;
  version: string;
}) {
  const [matches, setMatches] = useState<PropertyMatch[] | null>(null);
  const [failed, setFailed] = useState(false);
  const [open, setOpen] = useState(true);

  useEffect(() => {
    setMatches(null);
    setFailed(false);
    fetchRequirementMatches(clientId, requirementId)
      .then(setMatches)
      .catch(() => setFailed(true));
  }, [clientId, requirementId, version]);

  if (failed) {
    return (
      <p className="mt-3 text-[12px] text-critical">
        Nie udało się sprawdzić pasujących ofert.
      </p>
    );
  }

  if (matches === null) {
    return <p className="mt-3 text-[12px] text-ink-muted">Szukanie pasujących ofert…</p>;
  }

  if (matches.length === 0) {
    return (
      <p className="mt-3 border-t border-line pt-3 text-[12px] text-ink-muted">
        Brak pasujących ofert w biurze.
      </p>
    );
  }

  return (
    <div className="mt-3 border-t border-line pt-3">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        className="flex items-center gap-1.5 text-[12px] font-medium text-accent hover:text-accent-hover"
      >
        <ChevronDown
          className={cn("size-3.5 transition-transform", open && "rotate-180")}
          strokeWidth={2.25}
        />
        Pasujące oferty: {matches.length}
      </button>

      {open && (
        <ul className="mt-2 flex flex-col gap-2">
          {matches.map(({ property: p, criteria }) => {
            const note = STATUS_NOTE[p.status];
            return (
              <li key={p.id} className="rounded-md border border-line bg-surface px-3 py-2.5">
                <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
                  <div className="flex min-w-0 flex-wrap items-baseline gap-x-2 gap-y-1">
                    <span className="font-mono text-[11px] text-ink-muted">
                      {p.referenceNumber}
                    </span>
                    <Link
                      to={`/nieruchomosci/${p.id}`}
                      className="text-[13px] font-medium text-ink hover:text-accent"
                    >
                      {p.title}
                    </Link>
                    {note && <Badge tone={note.tone}>{note.label}</Badge>}
                  </div>
                  <span className="text-[13px] font-medium tabular-nums text-ink">
                    {formatCurrency(p.price)}
                    <span className="ml-1 text-[12px] font-normal text-ink-muted">
                      · {formatNumber(p.totalArea)} m²
                    </span>
                  </span>
                </div>
                <p className="mt-0.5 mb-2 text-[12px] text-ink-secondary">
                  {p.city}
                  {p.district && `, ${p.district}`}
                  {p.roomsCount != null && ` · pokoje: ${p.roomsCount}`}
                </p>
                <MatchCriteria criteria={criteria} />
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
