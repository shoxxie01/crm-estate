import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Pencil, Trash2 } from "lucide-react";
import {
  deleteProperty,
  fetchDictionaries,
  fetchProperty,
  type Dictionaries,
  type PropertyDetail,
} from "../../api/properties";
import { ApiError } from "../../api/client";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { formatCurrency, formatNumber } from "../../lib/format";

/** Status oferty → ton odznaki. Kolor zawsze w parze z etykietą. */
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

export function PropertyDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [property, setProperty] = useState<PropertyDetail | null>(null);
  const [dict, setDict] = useState<Dictionaries | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    fetchProperty(id)
      .then((result) => {
        setProperty(result);
        setError(null);
      })
      .catch((cause) =>
        setError(
          cause instanceof ApiError
            ? cause.message
            : "Nie udało się wczytać oferty.",
        ),
      )
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    fetchDictionaries().then(setDict).catch(() => undefined);
  }, []);

  // Jedna mapa etykiet ze wszystkich słowników — front trzyma wartości jako
  // techniczne nazwy, a tu tłumaczymy je na polskie etykiety do wyświetlenia.
  const labels = useMemo(() => {
    const map = new Map<string, string>();
    if (!dict) return map;
    const groups = [
      dict.propertyType, dict.transactionType, dict.marketType, dict.status,
      dict.currency, dict.voivodeship, dict.ownershipForm, dict.buildingType,
      dict.buildingMaterial, dict.constructionStatus, dict.windowsType,
      dict.roofType, dict.roofing, dict.garretType, dict.surroundings,
      dict.heatingType, dict.plotType, dict.roadAccess, dict.commercialUse,
      dict.hallStructure, dict.flooring, dict.parkingType, dict.energyClass,
      dict.garageType, dict.roomBathroom,
    ];
    for (const group of groups) for (const entry of group) map.set(entry.value, entry.label);
    for (const fg of dict.featureGroups) for (const entry of fg.features) map.set(entry.value, entry.label);
    return map;
  }, [dict]);

  const L = (value: string | null | undefined) =>
    value ? (labels.get(value) ?? value) : null;

  async function remove() {
    if (!id) return;
    setDeleting(true);
    setError(null);
    try {
      await deleteProperty(id);
      navigate("/nieruchomosci");
    } catch (cause) {
      setError(
        cause instanceof ApiError ? cause.message : "Nie udało się usunąć oferty.",
      );
      setDeleting(false);
    }
  }

  if (loading) {
    return <p className="text-[13px] text-ink-muted">Wczytywanie…</p>;
  }

  if (!property) {
    return (
      <div className="flex flex-col gap-3">
        <p className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {error ?? "Nie znaleziono oferty."}
        </p>
        <Button
          variant="secondary"
          size="sm"
          onClick={() => navigate("/nieruchomosci")}
        >
          <ArrowLeft className="size-4" strokeWidth={2} />
          Wróć do listy
        </Button>
      </div>
    );
  }

  const p = property;
  const type = p.propertyType;
  const isMieszkanie = type === "MIESZKANIE";
  const isDom = type === "DOM";
  const isDzialka = type === "DZIALKA";
  const isLokal = type === "LOKAL_UZYTKOWY";
  const isHala = type === "HALA_MAGAZYN";
  const isGaraz = type === "GARAZ";
  const isPokoj = type === "POKOJ";

  // Ta sama logika widoczności co w formularzu — szczegóły muszą pokazywać
  // dokładnie te pola, które dla danego typu mają sens.
  const showUsableArea = isMieszkanie || isDom || isLokal || isHala;
  const showPlotArea = isDom;
  const showRooms = isMieszkanie || isDom || isLokal;
  const roomsLabel = isLokal ? "Liczba pomieszczeń" : "Liczba pokoi";
  const showBaths = isMieszkanie || isDom || isLokal;
  const showFloor = isMieszkanie || isLokal || isPokoj || isGaraz;
  const floorLabel = isGaraz ? "Poziom" : "Piętro";
  const showBuildingFloors = isMieszkanie || isDom || isLokal;
  const showCeiling = isLokal || isHala;
  const totalAreaLabel = isDzialka
    ? "Powierzchnia działki"
    : isGaraz
      ? "Powierzchnia"
      : isPokoj
        ? "Powierzchnia pokoju"
        : "Powierzchnia całkowita";
  const showPricePerM2 = !isGaraz && !isPokoj;

  const buildingFull = isMieszkanie || isDom;
  const showBuildingSection = !isDzialka;
  const showBuildYear = isMieszkanie || isDom || isLokal || isHala;
  const showConstructionStatus = isMieszkanie || isDom || isLokal || isHala;
  const showOwnershipForm = isMieszkanie || isDom || isLokal || isHala || isGaraz;
  const showFurnished = isMieszkanie || isDom || isPokoj;
  const showAvailableFrom = !isDzialka;
  const buildingTitle = isGaraz || isPokoj ? "Szczegóły" : "Budynek";

  const showLandSection = isDom || isDzialka;
  const showCommercialSection = isLokal || isHala;
  const showEnergy = isMieszkanie || isDom || isLokal || isHala;

  return (
    <div className="flex max-w-4xl flex-col gap-4 pb-10">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate("/nieruchomosci")}
          >
            <ArrowLeft className="size-4" strokeWidth={2} />
            Wróć
          </Button>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-[12px] text-ink-muted">
                {p.referenceNumber}
              </span>
              <Badge tone={statusTones[p.status] ?? "neutral"}>
                {L(p.status)}
              </Badge>
            </div>
            <h1 className="text-base font-semibold tracking-tight text-ink">
              {p.title}
            </h1>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => navigate(`/nieruchomosci/${p.id}/edytuj`)}
            disabled={deleting}
          >
            <Pencil className="size-4" strokeWidth={2} />
            Edytuj
          </Button>

          {confirmDelete ? (
            <span className="flex items-center gap-2 text-[13px] text-ink-secondary">
              Usunąć ofertę?
              <Button
                size="sm"
                variant="secondary"
                onClick={remove}
                disabled={deleting}
                className="border-critical/40 text-critical hover:bg-critical/8"
              >
                {deleting ? "Usuwanie…" : "Tak, usuń"}
              </Button>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => setConfirmDelete(false)}
                disabled={deleting}
              >
                Anuluj
              </Button>
            </span>
          ) : (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setConfirmDelete(true)}
              className="text-critical hover:bg-critical/8"
            >
              <Trash2 className="size-4" strokeWidth={2} />
              Usuń
            </Button>
          )}
        </div>
      </header>

      {error && (
        <p className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {error}
        </p>
      )}

      <Section title="Klasyfikacja i cena">
        <Field label="Rodzaj" value={L(p.propertyType)} />
        <Field label="Transakcja" value={L(p.transactionType)} />
        <Field label="Rynek" value={L(p.marketType)} />
        <Field
          label="Cena"
          value={`${formatCurrency(p.pricing.price)}${p.pricing.priceNegotiable ? " (do negocjacji)" : ""}`}
        />
        {showPricePerM2 && (
          <Field
            label="Cena za m²"
            value={
              p.pricing.pricePerSquareMeter
                ? `${formatNumber(Math.round(p.pricing.pricePerSquareMeter))} zł/m²`
                : null
            }
          />
        )}
        <Field
          label="Czynsz administracyjny"
          value={p.pricing.rent != null ? formatCurrency(p.pricing.rent) : null}
        />
        <Field
          label="Kaucja"
          value={p.pricing.deposit != null ? formatCurrency(p.pricing.deposit) : null}
        />
        <Field
          label="Prowizja biura"
          value={
            p.pricing.commissionPercent != null
              ? `${formatNumber(p.pricing.commissionPercent)}%`
              : null
          }
        />
      </Section>

      <Section title="Powierzchnia i układ">
        <Field label={totalAreaLabel} value={`${formatNumber(p.area.totalArea)} m²`} />
        {showUsableArea && <Field label="Powierzchnia użytkowa" value={area(p.area.usableArea)} />}
        {showPlotArea && <Field label="Powierzchnia działki" value={area(p.area.plotArea)} />}
        {isGaraz && <Field label="Typ" value={L(p.garageType)} />}
        {showRooms && <Field label={roomsLabel} value={numOrNull(p.area.roomsCount)} />}
        {showBaths && <Field label="Liczba łazienek" value={numOrNull(p.area.bathroomsCount)} />}
        {isPokoj && <Field label="Dla ilu osób" value={numOrNull(p.occupants)} />}
        {isPokoj && <Field label="Łazienka" value={L(p.roomBathroom)} />}
        {showFloor && <Field label={floorLabel} value={floor(p.area.floorNo)} />}
        {showBuildingFloors && <Field label="Liczba pięter" value={numOrNull(p.area.buildingFloorsCount)} />}
        {showCeiling && (
          <Field
            label="Wysokość pomieszczeń"
            value={p.area.ceilingHeight != null ? `${formatNumber(p.area.ceilingHeight)} m` : null}
          />
        )}
        {showAvailableFrom && <Field label="Dostępna od" value={p.availableFrom} />}
      </Section>

      <Section title="Lokalizacja">
        <Field label="Województwo" value={L(p.address.voivodeship)} />
        <Field label="Powiat" value={p.address.county} />
        <Field label="Gmina" value={p.address.commune} />
        <Field label="Miejscowość" value={p.address.city} />
        <Field label="Dzielnica" value={p.address.district} />
        <Field label="Ulica" value={p.address.street} />
        <Field label="Numer budynku" value={p.address.buildingNumber} />
        <Field label="Kod pocztowy" value={p.address.postalCode} />
        <Field
          label="Dokładny adres w ogłoszeniu"
          value={p.address.hideExactAddress ? "Ukryty" : "Widoczny"}
        />
      </Section>

      {showBuildingSection && (
        <Section title={buildingTitle}>
          {showBuildYear && <Field label="Rok budowy" value={numOrNull(p.building.buildYear)} />}
          {buildingFull && <Field label="Rodzaj zabudowy" value={L(p.building.buildingType)} />}
          {buildingFull && <Field label="Materiał" value={L(p.building.buildingMaterial)} />}
          {showConstructionStatus && <Field label="Stan wykończenia" value={L(p.building.constructionStatus)} />}
          {showOwnershipForm && <Field label="Forma własności" value={L(p.building.ownershipForm)} />}
          {buildingFull && <Field label="Okna" value={L(p.building.windowsType)} />}
          {buildingFull && <Field label="Położenie" value={L(p.building.surroundings)} />}
          {showFurnished && <Field label="Umeblowane" value={bool(p.building.furnished)} />}
        </Section>
      )}

      {showLandSection && (
        <Section title="Działka">
          <Field label="Typ działki" value={L(p.land.plotType)} />
          <Field label="Wymiary" value={p.land.dimensions} />
          <Field label="Dojazd" value={L(p.land.roadAccess)} />
          <Field label="Ogrodzona" value={bool(p.land.fenced)} />
          <Field label="Plan miejscowy" value={p.land.zoningPlan} />
        </Section>
      )}

      {showCommercialSection && (
        <Section title={isHala ? "Hala / magazyn" : "Lokal użytkowy"}>
          {isHala && <Field label="Konstrukcja" value={L(p.commercial.structure)} />}
          {isHala && <Field label="Posadzka" value={L(p.commercial.flooring)} />}
          <Field label="Parking" value={L(p.commercial.parkingType)} />
          {isHala && (
            <Field
              label="Moc przyłącza"
              value={p.commercial.powerConnectionKw != null ? `${formatNumber(p.commercial.powerConnectionKw)} kW` : null}
            />
          )}
          {isHala && (
            <Field
              label="Nośność posadzki"
              value={p.commercial.floorLoadPerM2 != null ? `${formatNumber(p.commercial.floorLoadPerM2)} t/m²` : null}
            />
          )}
          {isHala && <Field label="Liczba bram / doków" value={numOrNull(p.commercial.loadingDocksCount)} />}
          <Field label="Pomieszczenia biurowe" value={bool(p.commercial.officeSpace)} />
          <Field label="Zaplecze socjalne" value={bool(p.commercial.socialFacilities)} />
          {isHala && <Field label="Rampa" value={bool(p.commercial.loadingRamp)} />}
        </Section>
      )}

      {showEnergy && (
      <Section title="Charakterystyka energetyczna">
        {p.energy.exempt ? (
          <Field label="Zwolnienie" value={p.energy.exemptNote ?? "Tak"} />
        ) : (
          <>
            <Field
              label="EP — energia pierwotna"
              value={p.energy.energyPrimary != null ? `${formatNumber(p.energy.energyPrimary)} kWh/(m²·rok)` : null}
            />
            <Field
              label="EK — energia końcowa"
              value={p.energy.energyFinal != null ? `${formatNumber(p.energy.energyFinal)} kWh/(m²·rok)` : null}
            />
            <Field label="Klasa energetyczna" value={L(p.energy.energyClass)} />
            <Field label="Numer świadectwa" value={p.energy.certificateNumber} />
          </>
        )}
      </Section>
      )}

      {(p.features.length > 0 ||
        p.heatingTypes.length > 0 ||
        p.commercialUses.length > 0) && (
        <section className="card">
          <header className="border-b border-line px-4 py-3">
            <h2 className="text-[13px] font-semibold tracking-tight text-ink">
              Cechy
            </h2>
          </header>
          <div className="flex flex-col gap-3 p-4">
            <Chips title="Ogrzewanie" values={p.heatingTypes} label={L} />
            <Chips title="Przeznaczenie" values={p.commercialUses} label={L} />
            <Chips title="Udogodnienia" values={p.features} label={L} />
          </div>
        </section>
      )}

      <section className="card">
        <header className="border-b border-line px-4 py-3">
          <h2 className="text-[13px] font-semibold tracking-tight text-ink">
            Opis
          </h2>
        </header>
        <p className="p-4 text-[13px] leading-relaxed whitespace-pre-wrap text-ink-secondary">
          {p.description}
        </p>
      </section>

      {(p.keysInfo || p.privateNotes || p.videoUrl || p.panoramaUrl) && (
        <Section title="Materiały i notatki">
          <Field label="Film" value={p.videoUrl} />
          <Field label="Wirtualny spacer" value={p.panoramaUrl} />
          <Field label="Klucze" value={p.keysInfo} />
          <Field label="Notatki wewnętrzne" value={p.privateNotes} />
        </Section>
      )}
    </div>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="card">
      <header className="border-b border-line px-4 py-3">
        <h2 className="text-[13px] font-semibold tracking-tight text-ink">
          {title}
        </h2>
      </header>
      <dl className="grid gap-x-8 gap-y-3 p-4 text-[13px] sm:grid-cols-2 md:grid-cols-3">
        {children}
      </dl>
    </section>
  );
}

function Field({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-[11px] font-medium tracking-wide text-ink-muted uppercase">
        {label}
      </dt>
      <dd className="text-ink">
        {value ? value : <span className="text-ink-muted">—</span>}
      </dd>
    </div>
  );
}

function Chips({
  title,
  values,
  label,
}: {
  title: string;
  values: string[];
  label: (v: string) => string | null;
}) {
  if (values.length === 0) return null;
  return (
    <div className="flex flex-col gap-1.5">
      <span className="text-[11px] font-medium tracking-wide text-ink-muted uppercase">
        {title}
      </span>
      <div className="flex flex-wrap gap-1.5">
        {values.map((v) => (
          <Badge key={v} tone="neutral">
            {label(v) ?? v}
          </Badge>
        ))}
      </div>
    </div>
  );
}

const area = (v: number | null): string | null =>
  v != null ? `${formatNumber(v)} m²` : null;

const numOrNull = (v: number | null): string | null =>
  v != null ? formatNumber(v) : null;

const bool = (v: boolean | null): string | null =>
  v == null ? null : v ? "Tak" : "Nie";

const floor = (v: number | null): string | null => {
  if (v == null) return null;
  if (v === -1) return "Suterena";
  if (v === 0) return "Parter";
  return String(v);
};
