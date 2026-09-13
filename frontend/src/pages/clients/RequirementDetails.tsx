import { useEffect, useMemo, useState } from "react";
import { Check, MapPin, Star } from "lucide-react";
import {
  fetchClientDictionaries,
  type ClientDictionaries,
} from "../../api/clients";
import { fetchDictionaries, type Dictionaries } from "../../api/properties";
import { Badge } from "../../components/ui/Badge";
import { cn } from "../../lib/cn";
import {
  SEEKER_TRANSACTION,
  formatAreaRange,
  formatCountRange,
  formatFloorRange,
  formatLocalDate,
  formatPriceRange,
} from "./requirementMeta";

/**
 * Kryteria poszukiwania w kształcie wspólnym dla zapisanego poszukiwania
 * i zgłoszenia z formularza. W zgłoszeniu serwer pomija puste pola, stąd
 * wszystko poza transakcją i rodzajem jest opcjonalne.
 */
export interface RequirementCriteria {
  transactionType: string;
  propertyTypes: string[];
  marketType?: string | null;
  locations?: { city: string; district?: string | null }[] | null;
  priceMin?: number | null;
  priceMax?: number | null;
  areaMin?: number | null;
  areaMax?: number | null;
  roomsMin?: number | null;
  roomsMax?: number | null;
  floorMin?: number | null;
  floorMax?: number | null;
  excludeTopFloor?: boolean | null;
  financing?: string | null;
  moveInDate?: string | null;
  requiredFeatures?: string[] | null;
  preferredFeatures?: string[] | null;
}

export interface RequirementLabels {
  propertyType: Map<string, string>;
  marketType: Map<string, string>;
  feature: Map<string, string>;
  financing: Map<string, string>;
}

/** Słowniki potrzebne do opisu i edycji poszukiwań, razem z mapami etykiet. */
export function useRequirementDictionaries() {
  const [propertyDict, setPropertyDict] = useState<Dictionaries | null>(null);
  const [clientDict, setClientDict] = useState<ClientDictionaries | null>(null);

  useEffect(() => {
    fetchDictionaries().then(setPropertyDict).catch(() => undefined);
    fetchClientDictionaries().then(setClientDict).catch(() => undefined);
  }, []);

  const labels = useMemo<RequirementLabels>(() => {
    const map = (entries: { value: string; label: string }[] = []) =>
      new Map(entries.map((e) => [e.value, e.label]));
    return {
      propertyType: map(propertyDict?.propertyType),
      marketType: map(propertyDict?.marketType),
      feature: map(propertyDict?.featureGroups.flatMap((g) => g.features)),
      financing: map(clientDict?.financing),
    };
  }, [propertyDict, clientDict]);

  return { propertyDict, clientDict, labels };
}

const label = (map: Map<string, string>, value: string) => map.get(value) ?? value;

/** Nagłówek: „Kupno" + rodzaje, pod spodem lokalizacje. */
export function RequirementHeadline({
  criteria: c,
  labels,
  muted = false,
}: {
  criteria: RequirementCriteria;
  labels: RequirementLabels;
  muted?: boolean;
}) {
  const locations = c.locations ?? [];
  return (
    <div className="min-w-0">
      <div className="flex flex-wrap items-center gap-2">
        <Badge tone={c.transactionType === "SALE" ? "good" : "neutral"}>
          {SEEKER_TRANSACTION[c.transactionType] ?? c.transactionType}
        </Badge>
        <h3
          className={cn(
            "text-[13px] font-semibold tracking-tight",
            muted ? "text-ink-secondary" : "text-ink",
          )}
        >
          {c.propertyTypes.map((t) => label(labels.propertyType, t)).join(", ")}
        </h3>
      </div>
      <p className="mt-1 flex items-start gap-1 text-[12px] text-ink-secondary">
        <MapPin className="mt-px size-3.5 shrink-0 text-ink-muted" strokeWidth={2} />
        {locations.length
          ? locations
              .map((l) => (l.district ? `${l.city} — ${l.district}` : l.city))
              .join(" · ")
          : "Lokalizacja obojętna"}
      </p>
    </div>
  );
}

/** Zakresy, rynek, finansowanie, termin i cechy — tylko to, co podano. */
export function RequirementFacts({
  criteria: c,
  labels,
}: {
  criteria: RequirementCriteria;
  labels: RequirementLabels;
}) {
  const purchase = c.transactionType === "SALE";
  const required = c.requiredFeatures ?? [];
  const preferred = c.preferredFeatures ?? [];

  const facts: [string, string | null | undefined][] = [
    [purchase ? "Budżet" : "Czynsz", formatPriceRange(c.priceMin ?? null, c.priceMax ?? null)],
    ["Metraż", formatAreaRange(c.areaMin ?? null, c.areaMax ?? null)],
    ["Pokoje", formatCountRange(c.roomsMin ?? null, c.roomsMax ?? null)],
    ["Piętro", formatFloorRange(c.floorMin ?? null, c.floorMax ?? null, Boolean(c.excludeTopFloor))],
    ["Rynek", c.marketType && label(labels.marketType, c.marketType)],
    ["Finansowanie", c.financing && label(labels.financing, c.financing)],
    [purchase ? "Termin zakupu" : "Wprowadzenie od", c.moveInDate && formatLocalDate(c.moveInDate)],
  ];
  const shown = facts.filter((fact): fact is [string, string] => Boolean(fact[1]));

  return (
    <>
      {shown.length > 0 && (
        <dl className="mt-3 grid grid-cols-2 gap-x-6 gap-y-2 text-[13px] sm:grid-cols-4">
          {shown.map(([name, value]) => (
            <div key={name} className="flex flex-col gap-0.5">
              <dt className="text-[11px] font-medium tracking-wide text-ink-muted uppercase">
                {name}
              </dt>
              <dd className="tabular-nums text-ink">{value}</dd>
            </div>
          ))}
        </dl>
      )}

      {(required.length > 0 || preferred.length > 0) && (
        <div className="mt-3 flex flex-wrap gap-1.5">
          {required.map((f) => (
            <span
              key={f}
              className="inline-flex items-center gap-1 rounded border border-accent-ring/60 bg-accent-subtle px-1.5 py-0.5 text-[11px] font-medium text-accent"
              title="Musi mieć"
            >
              <Check className="size-3" strokeWidth={2.5} />
              {label(labels.feature, f)}
            </span>
          ))}
          {preferred.map((f) => (
            <span
              key={f}
              className="inline-flex items-center gap-1 rounded border border-dashed border-line-strong px-1.5 py-0.5 text-[11px] text-ink-secondary"
              title="Mile widziane"
            >
              <Star className="size-3 text-ink-muted" strokeWidth={2} />
              {label(labels.feature, f)}
            </span>
          ))}
        </div>
      )}
    </>
  );
}
