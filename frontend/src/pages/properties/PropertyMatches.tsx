import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Phone, UserSearch } from "lucide-react";
import { fetchPropertyMatches, type ClientMatch } from "../../api/matching";
import { ApiError } from "../../api/client";
import { MatchCriteria } from "../../components/MatchCriteria";
import {
  SEEKER_TRANSACTION,
  formatAreaRange,
  formatPriceRange,
} from "../clients/requirementMeta";

/** Statusy, dla których serwer w ogóle szuka klientów. Jak MatchingService.AVAILABLE. */
const AVAILABLE = new Set(["DRAFT", "ACTIVE", "RESERVED"]);

/**
 * „Kto szuka czegoś takiego". Klienci z aktywnym poszukiwaniem pasującym do
 * tej oferty. Z kontaktem od razu pod ręką, bo następny krok to telefon.
 */
export function PropertyMatches({
  propertyId,
  status,
  label,
}: {
  propertyId: string;
  status: string;
  label: (value: string | null | undefined) => string | null;
}) {
  const [matches, setMatches] = useState<ClientMatch[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setMatches(null);
    fetchPropertyMatches(propertyId)
      .then((result) => {
        setMatches(result);
        setError(null);
      })
      .catch((cause) =>
        setError(
          cause instanceof ApiError
            ? cause.message
            : "Nie udało się wczytać pasujących klientów.",
        ),
      );
  }, [propertyId, status]);

  const available = AVAILABLE.has(status);

  return (
    <section className="card">
      <header className="border-b border-line px-4 py-3">
        <h2 className="text-[13px] font-semibold tracking-tight text-ink">
          Pasujący klienci
          {matches && matches.length > 0 && (
            <span className="ml-1.5 font-normal text-ink-muted tabular-nums">
              {matches.length}
            </span>
          )}
        </h2>
        <p className="mt-0.5 text-[12px] text-ink-muted">
          Klienci, których aktywne poszukiwanie pasuje do tej oferty.
        </p>
      </header>

      {error ? (
        <p className="m-4 rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {error}
        </p>
      ) : !available ? (
        <p className="px-4 py-6 text-center text-[13px] text-ink-secondary">
          Oferta nie jest już dostępna, więc nie szukamy dla niej klientów.
        </p>
      ) : matches === null ? (
        <p className="px-4 py-6 text-center text-[13px] text-ink-muted">Szukanie…</p>
      ) : matches.length === 0 ? (
        <div className="px-4 py-6 text-center">
          <UserSearch className="mx-auto mb-2 size-5 text-ink-muted" strokeWidth={1.5} />
          <p className="text-[13px] text-ink-secondary">
            Żadne aktywne poszukiwanie nie pasuje do tej oferty.
          </p>
        </div>
      ) : (
        <ul className="divide-y divide-line">
          {matches.map((match) => {
            const r = match.requirement;
            const summary = [
              SEEKER_TRANSACTION[r.transactionType],
              r.propertyTypes.map((t) => label(t) ?? t).join(", "),
              r.locations.length
                ? r.locations
                    .map((l) => (l.district ? `${l.city}. ${l.district}` : l.city))
                    .join(" · ")
                : null,
              formatPriceRange(r.priceMin, r.priceMax),
              formatAreaRange(r.areaMin, r.areaMax),
            ].filter(Boolean);

            return (
              <li key={r.id} className="flex flex-col gap-2 px-4 py-3">
                <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
                  <Link
                    to={`/klienci/${match.clientId}`}
                    className="text-[13px] font-semibold text-ink hover:text-accent"
                  >
                    {match.clientName}
                  </Link>
                  <div className="flex items-center gap-3 text-[12px] text-ink-secondary">
                    {match.phone && (
                      <a
                        href={`tel:${match.phone.replace(/\s/g, "")}`}
                        className="inline-flex items-center gap-1 tabular-nums hover:text-accent"
                      >
                        <Phone className="size-3" strokeWidth={2} />
                        {match.phone}
                      </a>
                    )}
                    <span className="text-ink-muted">opiekun: {match.agentName}</span>
                  </div>
                </div>
                <p className="text-[12px] text-ink-secondary">{summary.join(" · ")}</p>
                <MatchCriteria criteria={match.criteria} />
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
