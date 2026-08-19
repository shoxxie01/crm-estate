import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AlertTriangle, Building2, CheckCircle2, Plus } from "lucide-react";
import {
  fetchDictionaries,
  fetchProperties,
  type Dictionaries,
  type PageResponse,
  type PropertySummary,
} from "../../api/properties";
import { ApiError } from "../../api/client";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Select } from "../../components/ui/Select";
import { formatCurrency, formatNumber } from "../../lib/format";

/** Status oferty → ton odznaki. Kolor zawsze w parze z etykietą, nigdy sam. */
const statusTones: Record<
  string,
  "neutral" | "accent" | "good" | "warning" | "critical"
> = {
  ROBOCZA: "neutral",
  AKTYWNA: "good",
  ZAREZERWOWANA: "warning",
  SPRZEDANA: "accent",
  WYNAJETA: "accent",
  ARCHIWALNA: "neutral",
};

export function PropertiesPage() {
  const navigate = useNavigate();
  const [dictionaries, setDictionaries] = useState<Dictionaries | null>(null);
  const [page, setPage] = useState<PageResponse<PropertySummary> | null>(null);
  const [status, setStatus] = useState("");
  const [type, setType] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [dictError, setDictError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDictionaries()
      .then(setDictionaries)
      .catch(() =>
        // Bez słowników filtry byłyby puste i wyglądałyby na zepsute —
        // lepiej powiedzieć wprost, że to one się nie wczytały.
        setDictError("Nie udało się wczytać słowników — listy wyboru są puste."),
      );
  }, []);

  useEffect(() => {
    setLoading(true);
    fetchProperties({ status: status || undefined, type: type || undefined })
      .then((result) => {
        setPage(result);
        setError(null);
      })
      .catch((cause) =>
        setError(
          cause instanceof ApiError
            ? cause.message
            : "Nie udało się wczytać ofert.",
        ),
      )
      .finally(() => setLoading(false));
  }, [status, type]);

  // Słowniki przychodzą jako listy {value,label} — mapa daje szybki podgląd etykiety.
  const labels = useMemo(() => {
    const map = new Map<string, string>();
    if (!dictionaries) return map;
    for (const group of [
      dictionaries.propertyType,
      dictionaries.transactionType,
      dictionaries.status,
      dictionaries.marketType,
    ]) {
      for (const entry of group) map.set(entry.value, entry.label);
    }
    return map;
  }, [dictionaries]);

  const label = (value: string) => labels.get(value) ?? value;

  return (
    <div className="flex flex-col gap-4">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-base font-semibold tracking-tight text-ink">
            Nieruchomości
          </h1>
          <p className="mt-0.5 text-[13px] text-ink-secondary">
            {page
              ? `${formatNumber(page.totalElements)} ${ofertaForm(page.totalElements)} w bazie biura`
              : "Wczytywanie…"}
          </p>
        </div>

        <div className="flex items-end gap-2">
          <Select
            label="Status"
            placeholder="Wszystkie"
            options={dictionaries?.status ?? []}
            value={status}
            onChange={(event) => setStatus(event.target.value)}
            className="w-40"
          />
          <Select
            label="Rodzaj"
            placeholder="Wszystkie"
            options={dictionaries?.propertyType ?? []}
            value={type}
            onChange={(event) => setType(event.target.value)}
            className="w-44"
          />
          <Link to="/nieruchomosci/nowa">
            <Button>
              <Plus className="size-4" strokeWidth={2} />
              Dodaj ofertę
            </Button>
          </Link>
        </div>
      </header>

      {(error || dictError) && (
        <p className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {error ?? dictError}
        </p>
      )}

      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[900px] border-collapse text-[13px]">
            <thead>
              <tr className="border-b border-line-strong text-left text-[11px] font-medium tracking-wide text-ink-muted uppercase">
                <th className="px-4 py-2.5 font-medium">Numer</th>
                <th className="px-4 py-2.5 font-medium">Oferta</th>
                <th className="px-4 py-2.5 font-medium">Rodzaj</th>
                <th className="px-4 py-2.5 text-right font-medium">Cena</th>
                <th className="px-4 py-2.5 text-right font-medium">Pow.</th>
                <th className="px-4 py-2.5 text-right font-medium">zł/m²</th>
                <th className="px-4 py-2.5 font-medium">Status</th>
                <th className="px-4 py-2.5 font-medium">Eksport</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={8} className="px-4 py-10 text-center text-ink-muted">
                    Wczytywanie…
                  </td>
                </tr>
              )}

              {!loading && page?.content.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-4 py-12 text-center">
                    <Building2
                      className="mx-auto mb-2 size-6 text-ink-muted"
                      strokeWidth={1.5}
                    />
                    <p className="text-[13px] text-ink-secondary">
                      Brak ofert spełniających kryteria.
                    </p>
                  </td>
                </tr>
              )}

              {!loading &&
                page?.content.map((property) => (
                  <tr
                    key={property.id}
                    onClick={() => navigate(`/nieruchomosci/${property.id}`)}
                    className="cursor-pointer border-b border-line last:border-0 hover:bg-subtle"
                  >
                    <td className="px-4 py-2.5 font-mono text-[12px] text-ink-secondary">
                      {property.referenceNumber}
                    </td>
                    <td className="px-4 py-2.5">
                      <div className="font-medium text-ink">{property.title}</div>
                      <div className="text-[12px] text-ink-muted">
                        {property.city}
                        {property.district && `, ${property.district}`}
                      </div>
                    </td>
                    <td className="px-4 py-2.5 text-ink-secondary">
                      {label(property.propertyType)}
                      <span className="text-ink-muted">
                        {" · "}
                        {label(property.transactionType)}
                      </span>
                    </td>
                    <td className="px-4 py-2.5 text-right font-medium tabular-nums text-ink">
                      {formatCurrency(property.price)}
                    </td>
                    <td className="px-4 py-2.5 text-right tabular-nums text-ink-secondary">
                      {formatNumber(property.totalArea)} m²
                    </td>
                    <td className="px-4 py-2.5 text-right tabular-nums text-ink-secondary">
                      {property.pricePerSquareMeter
                        ? formatNumber(Math.round(property.pricePerSquareMeter))
                        : "—"}
                    </td>
                    <td className="px-4 py-2.5">
                      <Badge tone={statusTones[property.status] ?? "neutral"}>
                        {label(property.status)}
                      </Badge>
                    </td>
                    <td className="px-4 py-2.5">
                      {property.readyForExport ? (
                        <Badge
                          tone="good"
                          icon={<CheckCircle2 className="size-3" strokeWidth={2} />}
                        >
                          Gotowa
                        </Badge>
                      ) : (
                        <Badge
                          tone="warning"
                          icon={<AlertTriangle className="size-3" strokeWidth={2} />}
                        >
                          Niekompletna
                        </Badge>
                      )}
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function ofertaForm(count: number): string {
  if (count === 1) return "oferta";
  const rest = count % 10;
  const teens = count % 100;
  if (rest >= 2 && rest <= 4 && (teens < 12 || teens > 14)) return "oferty";
  return "ofert";
}
