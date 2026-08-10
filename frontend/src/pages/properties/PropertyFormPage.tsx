import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Info } from "lucide-react";
import {
  createProperty,
  fetchDictionaries,
  type Dictionaries,
} from "../../api/properties";
import { ApiError } from "../../api/client";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { Textarea } from "../../components/ui/Textarea";

/** Wszystkie pola tekstowe trzymamy jako stringi — konwersja dopiero przy wysyłce. */
type Fields = Record<string, string>;

const INITIAL: Fields = {
  propertyType: "MIESZKANIE",
  transactionType: "SPRZEDAZ",
  marketType: "WTORNY",
  status: "ROBOCZA",
  title: "",
  description: "",
  price: "",
  priceCurrency: "PLN",
  rent: "",
  deposit: "",
  commissionPercent: "",
  totalArea: "",
  usableArea: "",
  plotArea: "",
  roomsCount: "",
  bathroomsCount: "",
  floorNo: "",
  buildingFloorsCount: "",
  ceilingHeight: "",
  voivodeship: "",
  county: "",
  commune: "",
  city: "",
  district: "",
  street: "",
  buildingNumber: "",
  postalCode: "",
  buildYear: "",
  buildingType: "",
  buildingMaterial: "",
  constructionStatus: "",
  ownershipForm: "",
  windowsType: "",
  surroundings: "",
  plotType: "",
  plotDimensions: "",
  roadAccess: "",
  zoningPlan: "",
  hallStructure: "",
  flooring: "",
  parkingType: "",
  energyPrimary: "",
  energyFinal: "",
  energyClass: "",
  energyCertNumber: "",
  availableFrom: "",
  videoUrl: "",
  panoramaUrl: "",
  keysInfo: "",
  privateNotes: "",
  energyExemptNote: "",
};

export function PropertyFormPage() {
  const navigate = useNavigate();
  const [dict, setDict] = useState<Dictionaries | null>(null);
  const [fields, setFields] = useState<Fields>(INITIAL);
  const [features, setFeatures] = useState<Set<string>>(new Set());
  const [heating, setHeating] = useState<Set<string>>(new Set());
  const [uses, setUses] = useState<Set<string>>(new Set());
  const [flags, setFlags] = useState({
    priceNegotiable: false,
    priceIncludesRent: false,
    hideExactAddress: true,
    furnished: false,
    plotFenced: false,
    officeSpace: false,
    socialFacilities: false,
    loadingRamp: false,
    energyExempt: false,
    exportable: true,
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [dictError, setDictError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const loadDictionaries = () => {
    setDictError(null);
    fetchDictionaries()
      .then(setDict)
      .catch(() =>
        // Bez słowników każda lista wyboru jest pusta. Cichy błąd wygląda wtedy
        // jak zepsuty formularz, więc mówimy wprost, co się stało.
        setDictError(
          "Nie udało się wczytać słowników — listy wyboru pozostaną puste.",
        ),
      );
  };

  useEffect(loadDictionaries, []);

  const set = (name: string) => (event: { target: { value: string } }) =>
    setFields((current) => ({ ...current, [name]: event.target.value }));

  const toggle = (
    collection: Set<string>,
    setter: (next: Set<string>) => void,
    value: string,
  ) => {
    const next = new Set(collection);
    if (next.has(value)) next.delete(value);
    else next.add(value);
    setter(next);
  };

  const type = fields.propertyType;
  const isRent = fields.transactionType === "WYNAJEM";
  const isLand = type === "DZIALKA";
  const isCommercial = type === "LOKAL_UZYTKOWY" || type === "HALA_MAGAZYN";
  const showBuilding = !isLand;
  const showLand = isLand || type === "DOM";
  // Otodom odrzuca mieszkanie i dom bez liczby pokoi.
  const roomsRequired = type === "MIESZKANIE" || type === "DOM";

  const options = useMemo(
    () => ({
      propertyType: dict?.propertyType ?? [],
      transactionType: dict?.transactionType ?? [],
      marketType: dict?.marketType ?? [],
      status: dict?.status ?? [],
      currency: dict?.currency ?? [],
      voivodeship: dict?.voivodeship ?? [],
      buildingType: dict?.buildingType ?? [],
      buildingMaterial: dict?.buildingMaterial ?? [],
      constructionStatus: dict?.constructionStatus ?? [],
      ownershipForm: dict?.ownershipForm ?? [],
      windowsType: dict?.windowsType ?? [],
      surroundings: dict?.surroundings ?? [],
      plotType: dict?.plotType ?? [],
      roadAccess: dict?.roadAccess ?? [],
      hallStructure: dict?.hallStructure ?? [],
      flooring: dict?.flooring ?? [],
      parkingType: dict?.parkingType ?? [],
      energyClass: dict?.energyClass ?? [],
    }),
    [dict],
  );

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setSaving(true);
    setErrors({});
    setFormError(null);

    try {
      const created = await createProperty({
        propertyType: fields.propertyType,
        transactionType: fields.transactionType,
        marketType: fields.marketType,
        status: fields.status,
        title: fields.title,
        description: fields.description,
        pricing: {
          price: num(fields.price) ?? 0,
          priceCurrency: fields.priceCurrency,
          priceNegotiable: flags.priceNegotiable,
          rent: num(fields.rent),
          priceIncludesRent: flags.priceIncludesRent,
          deposit: num(fields.deposit),
          commissionPercent: num(fields.commissionPercent),
        },
        area: {
          totalArea: num(fields.totalArea) ?? 0,
          usableArea: num(fields.usableArea),
          plotArea: num(fields.plotArea),
          roomsCount: int(fields.roomsCount),
          bathroomsCount: int(fields.bathroomsCount),
          floorNo: int(fields.floorNo),
          buildingFloorsCount: int(fields.buildingFloorsCount),
          ceilingHeight: num(fields.ceilingHeight),
        },
        address: {
          voivodeship: fields.voivodeship,
          county: blank(fields.county),
          commune: blank(fields.commune),
          city: fields.city,
          district: blank(fields.district),
          street: blank(fields.street),
          buildingNumber: blank(fields.buildingNumber),
          postalCode: blank(fields.postalCode),
          hideExactAddress: flags.hideExactAddress,
        },
        building: showBuilding
          ? {
              buildYear: int(fields.buildYear),
              buildingType: blank(fields.buildingType),
              buildingMaterial: blank(fields.buildingMaterial),
              constructionStatus: blank(fields.constructionStatus),
              ownershipForm: blank(fields.ownershipForm),
              windowsType: blank(fields.windowsType),
              surroundings: blank(fields.surroundings),
              furnished: flags.furnished,
            }
          : undefined,
        land: showLand
          ? {
              plotType: blank(fields.plotType),
              dimensions: blank(fields.plotDimensions),
              roadAccess: blank(fields.roadAccess),
              fenced: flags.plotFenced,
              zoningPlan: blank(fields.zoningPlan),
            }
          : undefined,
        commercial: isCommercial
          ? {
              structure: blank(fields.hallStructure),
              flooring: blank(fields.flooring),
              parkingType: blank(fields.parkingType),
              officeSpace: flags.officeSpace,
              socialFacilities: flags.socialFacilities,
              loadingRamp: flags.loadingRamp,
            }
          : undefined,
        energy: {
          energyPrimary: flags.energyExempt ? null : num(fields.energyPrimary),
          energyFinal: flags.energyExempt ? null : num(fields.energyFinal),
          energyClass: flags.energyExempt ? undefined : blank(fields.energyClass),
          certificateNumber: blank(fields.energyCertNumber),
          exempt: flags.energyExempt,
          exemptNote: blank(fields.energyExemptNote),
        },
        availableFrom: blank(fields.availableFrom) ?? null,
        features: [...features],
        heatingTypes: [...heating],
        commercialUses: [...uses],
        videoUrl: blank(fields.videoUrl),
        panoramaUrl: blank(fields.panoramaUrl),
        keysInfo: blank(fields.keysInfo),
        privateNotes: blank(fields.privateNotes),
        exportable: flags.exportable,
      });

      navigate("/nieruchomosci", { state: { created: created.id } });
    } catch (cause) {
      if (cause instanceof ApiError) {
        setErrors(cause.fieldErrors ?? {});
        setFormError(cause.message);
      } else {
        setFormError("Nie udało się zapisać oferty.");
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4 pb-10">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => navigate("/nieruchomosci")}
          >
            <ArrowLeft className="size-4" strokeWidth={2} />
            Wróć
          </Button>
          <h1 className="text-base font-semibold tracking-tight text-ink">
            Nowa oferta
          </h1>
        </div>

        <Button type="submit" disabled={saving}>
          {saving ? "Zapisywanie…" : "Zapisz ofertę"}
        </Button>
      </header>

      {formError && (
        <p className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {formError}
        </p>
      )}

      {dictError && (
        <p className="flex items-center justify-between gap-3 rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {dictError}
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={loadDictionaries}
          >
            Spróbuj ponownie
          </Button>
        </p>
      )}

      <Section
        title="Podstawowe"
        description="Pola wymagane przez portale ogłoszeniowe przy każdym rodzaju oferty."
      >
        <Select
          label="Rodzaj nieruchomości"
          options={options.propertyType}
          value={fields.propertyType}
          onChange={set("propertyType")}
          error={errors.propertyType}
        />
        <Select
          label="Typ transakcji"
          options={options.transactionType}
          value={fields.transactionType}
          onChange={set("transactionType")}
          error={errors.transactionType}
        />
        <Select
          label="Rynek"
          options={options.marketType}
          value={fields.marketType}
          onChange={set("marketType")}
          error={errors.marketType}
          hint="Wymagane przez Otodom."
        />
        <Select
          label="Status"
          options={options.status}
          value={fields.status}
          onChange={set("status")}
        />
        <div className="md:col-span-3">
          <Input
            label="Tytuł ogłoszenia"
            value={fields.title}
            onChange={set("title")}
            error={errors.title}
            hint="Do 50 znaków — dłuższy zostanie obcięty przez portal."
            maxLength={50}
            required
          />
        </div>
        <div className="md:col-span-3">
          <Textarea
            label="Opis"
            rows={6}
            value={fields.description}
            onChange={set("description")}
            error={errors.description}
            counter={{ value: fields.description.length, max: 20000 }}
            required
          />
        </div>
      </Section>

      <Section title="Cena">
        <Input
          label="Cena"
          type="number"
          step="0.01"
          value={fields.price}
          onChange={set("price")}
          error={errors["pricing.price"]}
          required
        />
        <Select
          label="Waluta"
          options={options.currency}
          value={fields.priceCurrency}
          onChange={set("priceCurrency")}
        />
        <Input
          label="Prowizja biura (%)"
          type="number"
          step="0.01"
          value={fields.commissionPercent}
          onChange={set("commissionPercent")}
          hint="Nie trafia do ogłoszenia."
        />
        <Input
          label="Czynsz administracyjny"
          type="number"
          step="0.01"
          value={fields.rent}
          onChange={set("rent")}
        />
        {isRent && (
          <Input
            label="Kaucja"
            type="number"
            step="0.01"
            value={fields.deposit}
            onChange={set("deposit")}
          />
        )}
        <div className="flex flex-col justify-end gap-2 pb-1">
          <Check
            label="Cena do negocjacji"
            checked={flags.priceNegotiable}
            onChange={(value) =>
              setFlags((f) => ({ ...f, priceNegotiable: value }))
            }
          />
          {isRent && (
            <Check
              label="Cena zawiera czynsz"
              checked={flags.priceIncludesRent}
              onChange={(value) =>
                setFlags((f) => ({ ...f, priceIncludesRent: value }))
              }
            />
          )}
        </div>
      </Section>

      <Section title="Powierzchnia i układ">
        <Input
          label="Powierzchnia całkowita (m²)"
          type="number"
          step="0.01"
          value={fields.totalArea}
          onChange={set("totalArea")}
          error={errors["area.totalArea"]}
          required
        />
        <Input
          label="Powierzchnia użytkowa (m²)"
          type="number"
          step="0.01"
          value={fields.usableArea}
          onChange={set("usableArea")}
        />
        {showLand && (
          <Input
            label="Powierzchnia działki (m²)"
            type="number"
            step="0.01"
            value={fields.plotArea}
            onChange={set("plotArea")}
          />
        )}
        {!isLand && (
          <>
            <Input
              label={`Liczba pokoi${roomsRequired ? "" : " (opcjonalnie)"}`}
              type="number"
              value={fields.roomsCount}
              onChange={set("roomsCount")}
              error={errors["area.roomsCount"]}
              hint={
                roomsRequired
                  ? "Wymagana — bez niej portal odrzuci ofertę."
                  : undefined
              }
              required={roomsRequired}
            />
            <Input
              label="Liczba łazienek"
              type="number"
              value={fields.bathroomsCount}
              onChange={set("bathroomsCount")}
            />
            <Input
              label="Piętro"
              type="number"
              value={fields.floorNo}
              onChange={set("floorNo")}
              error={errors["area.floorNo"]}
              hint="-1 = suterena, 0 = parter."
            />
            <Input
              label="Liczba pięter w budynku"
              type="number"
              value={fields.buildingFloorsCount}
              onChange={set("buildingFloorsCount")}
            />
          </>
        )}
        {isCommercial && (
          <Input
            label="Wysokość pomieszczeń (m)"
            type="number"
            step="0.01"
            value={fields.ceilingHeight}
            onChange={set("ceilingHeight")}
          />
        )}
      </Section>

      <Section
        title="Lokalizacja"
        description="Do zapisu wystarczą województwo i miejscowość. Powiat jest potrzebny dopiero do wysyłki na portal."
      >
        <Select
          label="Województwo"
          options={options.voivodeship}
          value={fields.voivodeship}
          onChange={set("voivodeship")}
          error={errors["address.voivodeship"]}
          placeholder="Wybierz…"
          required
        />
        <Input
          label="Powiat"
          value={fields.county}
          onChange={set("county")}
          error={errors["address.county"]}
          hint="Wymagany dopiero przy wysyłce na portal."
        />
        <Input
          label="Gmina"
          value={fields.commune}
          onChange={set("commune")}
        />
        <Input
          label="Miejscowość"
          value={fields.city}
          onChange={set("city")}
          error={errors["address.city"]}
          required
        />
        <Input
          label="Dzielnica"
          value={fields.district}
          onChange={set("district")}
        />
        <Input
          label="Kod pocztowy"
          value={fields.postalCode}
          onChange={set("postalCode")}
          error={errors["address.postalCode"]}
          placeholder="00-000"
        />
        <Input label="Ulica" value={fields.street} onChange={set("street")} />
        <Input
          label="Numer budynku"
          value={fields.buildingNumber}
          onChange={set("buildingNumber")}
        />
        <div className="flex items-end pb-2">
          <Check
            label="Ukryj dokładny adres w ogłoszeniu"
            checked={flags.hideExactAddress}
            onChange={(value) =>
              setFlags((f) => ({ ...f, hideExactAddress: value }))
            }
          />
        </div>
      </Section>

      {showBuilding && (
        <Section title="Budynek">
          <Input
            label="Rok budowy"
            type="number"
            value={fields.buildYear}
            onChange={set("buildYear")}
            error={errors["building.buildYear"]}
          />
          <Select
            label="Rodzaj zabudowy"
            options={options.buildingType}
            value={fields.buildingType}
            onChange={set("buildingType")}
            placeholder="Nie podano"
          />
          <Select
            label="Materiał budowy"
            options={options.buildingMaterial}
            value={fields.buildingMaterial}
            onChange={set("buildingMaterial")}
            placeholder="Nie podano"
          />
          <Select
            label="Stan wykończenia"
            options={options.constructionStatus}
            value={fields.constructionStatus}
            onChange={set("constructionStatus")}
            placeholder="Nie podano"
          />
          <Select
            label="Forma własności"
            options={options.ownershipForm}
            value={fields.ownershipForm}
            onChange={set("ownershipForm")}
            placeholder="Nie podano"
          />
          <Select
            label="Okna"
            options={options.windowsType}
            value={fields.windowsType}
            onChange={set("windowsType")}
            placeholder="Nie podano"
          />
          <Select
            label="Położenie"
            options={options.surroundings}
            value={fields.surroundings}
            onChange={set("surroundings")}
            placeholder="Nie podano"
          />
          <Input
            label="Dostępne od"
            type="date"
            value={fields.availableFrom}
            onChange={set("availableFrom")}
          />
          <div className="flex items-end pb-2">
            <Check
              label="Umeblowane"
              checked={flags.furnished}
              onChange={(value) => setFlags((f) => ({ ...f, furnished: value }))}
            />
          </div>
        </Section>
      )}

      {showLand && (
        <Section title="Działka">
          <Select
            label="Typ działki"
            options={options.plotType}
            value={fields.plotType}
            onChange={set("plotType")}
            placeholder="Nie podano"
          />
          <Input
            label="Wymiary"
            value={fields.plotDimensions}
            onChange={set("plotDimensions")}
            placeholder="np. 25x40"
            maxLength={32}
          />
          <Select
            label="Dojazd"
            options={options.roadAccess}
            value={fields.roadAccess}
            onChange={set("roadAccess")}
            placeholder="Nie podano"
          />
          <div className="md:col-span-2">
            <Input
              label="Przeznaczenie w planie miejscowym"
              value={fields.zoningPlan}
              onChange={set("zoningPlan")}
            />
          </div>
          <div className="flex items-end pb-2">
            <Check
              label="Ogrodzona"
              checked={flags.plotFenced}
              onChange={(value) => setFlags((f) => ({ ...f, plotFenced: value }))}
            />
          </div>
        </Section>
      )}

      {isCommercial && (
        <Section title="Lokal / hala">
          <Select
            label="Konstrukcja"
            options={options.hallStructure}
            value={fields.hallStructure}
            onChange={set("hallStructure")}
            placeholder="Nie podano"
          />
          <Select
            label="Posadzka"
            options={options.flooring}
            value={fields.flooring}
            onChange={set("flooring")}
            placeholder="Nie podano"
          />
          <Select
            label="Parking"
            options={options.parkingType}
            value={fields.parkingType}
            onChange={set("parkingType")}
            placeholder="Nie podano"
          />
          <div className="flex flex-col justify-end gap-2 pb-1">
            <Check
              label="Pomieszczenia biurowe"
              checked={flags.officeSpace}
              onChange={(value) => setFlags((f) => ({ ...f, officeSpace: value }))}
            />
            <Check
              label="Zaplecze socjalne"
              checked={flags.socialFacilities}
              onChange={(value) =>
                setFlags((f) => ({ ...f, socialFacilities: value }))
              }
            />
            <Check
              label="Rampa"
              checked={flags.loadingRamp}
              onChange={(value) => setFlags((f) => ({ ...f, loadingRamp: value }))}
            />
          </div>
          {dict && (
            <div className="md:col-span-3">
              <CheckGroup
                title="Przeznaczenie"
                entries={dict.commercialUse}
                selected={uses}
                onToggle={(value) => toggle(uses, setUses, value)}
              />
            </div>
          )}
        </Section>
      )}

      {!isLand && dict && (
        <Section title="Ogrzewanie">
          <div className="md:col-span-3">
            <CheckGroup
              title="Rodzaj ogrzewania"
              entries={dict.heatingType}
              selected={heating}
              onToggle={(value) => toggle(heating, setHeating, value)}
            />
          </div>
        </Section>
      )}

      <Section
        title="Świadectwo charakterystyki energetycznej"
        description="Obowiązkowe przy sprzedaży i najmie od 28.04.2023. Wskaźnik EP musi znaleźć się w ogłoszeniu."
      >
        <div className="md:col-span-3">
          <Check
            label="Budynek zwolniony z obowiązku posiadania świadectwa"
            checked={flags.energyExempt}
            onChange={(value) => setFlags((f) => ({ ...f, energyExempt: value }))}
          />
        </div>

        {flags.energyExempt ? (
          <div className="md:col-span-3">
            <Input
              label="Podstawa zwolnienia"
              value={fields.energyExemptNote}
              onChange={set("energyExemptNote")}
              error={errors["energy.exemptNote"]}
              hint="np. zabytek wpisany do rejestru, budynek do 50 m², obiekt sakralny."
            />
          </div>
        ) : (
          <>
            <Input
              label="EP — energia pierwotna"
              type="number"
              step="0.01"
              value={fields.energyPrimary}
              onChange={set("energyPrimary")}
              error={errors["energy.energyPrimary"]}
              hint="kWh/(m²·rok)"
            />
            <Input
              label="EK — energia końcowa"
              type="number"
              step="0.01"
              value={fields.energyFinal}
              onChange={set("energyFinal")}
              hint="kWh/(m²·rok)"
            />
            <Select
              label="Klasa energetyczna"
              options={options.energyClass}
              value={fields.energyClass}
              onChange={set("energyClass")}
              placeholder="Nie podano"
            />
            <Input
              label="Numer świadectwa"
              value={fields.energyCertNumber}
              onChange={set("energyCertNumber")}
            />
          </>
        )}
      </Section>

      {dict && (
        <Section title="Cechy">
          <div className="flex flex-col gap-5 md:col-span-3">
            {dict.featureGroups.map((group) => (
              <CheckGroup
                key={group.category}
                title={group.label}
                entries={group.features}
                selected={features}
                onToggle={(value) => toggle(features, setFeatures, value)}
              />
            ))}
          </div>
        </Section>
      )}

      <Section title="Materiały i notatki">
        <Input
          label="Link do filmu (YouTube)"
          value={fields.videoUrl}
          onChange={set("videoUrl")}
        />
        <Input
          label="Link do wirtualnego spaceru"
          value={fields.panoramaUrl}
          onChange={set("panoramaUrl")}
        />
        <Input
          label="Klucze"
          value={fields.keysInfo}
          onChange={set("keysInfo")}
          hint="Dane wewnętrzne — nie trafiają do ogłoszenia."
        />
        <div className="md:col-span-3">
          <Textarea
            label="Notatki wewnętrzne"
            rows={3}
            value={fields.privateNotes}
            onChange={set("privateNotes")}
          />
        </div>
        <div className="md:col-span-3">
          <Check
            label="Pozwól eksportować tę ofertę na portale"
            checked={flags.exportable}
            onChange={(value) => setFlags((f) => ({ ...f, exportable: value }))}
          />
        </div>
      </Section>

      <p className="flex items-start gap-2 text-[12px] text-ink-muted">
        <Info className="mt-px size-3.5 shrink-0" strokeWidth={2} />
        Zdjęcia dodaje się po zapisaniu oferty. Bez co najmniej jednego zdjęcia
        oferta nie zostanie uznana za gotową do eksportu.
      </p>

      <div className="flex justify-end gap-2">
        <Button
          type="button"
          variant="secondary"
          onClick={() => navigate("/nieruchomosci")}
        >
          Anuluj
        </Button>
        <Button type="submit" disabled={saving}>
          {saving ? "Zapisywanie…" : "Zapisz ofertę"}
        </Button>
      </div>
    </form>
  );
}

function Section({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children: ReactNode;
}) {
  return (
    <section className="card">
      <header className="border-b border-line px-4 py-3">
        <h2 className="text-[13px] font-semibold tracking-tight text-ink">
          {title}
        </h2>
        {description && (
          <p className="mt-0.5 text-[12px] text-ink-muted">{description}</p>
        )}
      </header>
      <div className="grid gap-4 p-4 md:grid-cols-3">{children}</div>
    </section>
  );
}

function Check({
  label,
  checked,
  onChange,
}: {
  label: string;
  checked: boolean;
  onChange: (value: boolean) => void;
}) {
  return (
    <label className="flex cursor-pointer items-center gap-2 text-[13px] text-ink">
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="size-4 rounded border-line text-accent focus:ring-2 focus:ring-accent-ring/50"
      />
      {label}
    </label>
  );
}

function CheckGroup({
  title,
  entries,
  selected,
  onToggle,
}: {
  title: string;
  entries: { value: string; label: string }[];
  selected: Set<string>;
  onToggle: (value: string) => void;
}) {
  return (
    <fieldset>
      <legend className="mb-2 text-[11px] font-medium tracking-wide text-ink-muted uppercase">
        {title}
      </legend>
      <div className="grid gap-x-4 gap-y-2 sm:grid-cols-2 lg:grid-cols-3">
        {entries.map((entry) => (
          <Check
            key={entry.value}
            label={entry.label}
            checked={selected.has(entry.value)}
            onChange={() => onToggle(entry.value)}
          />
        ))}
      </div>
    </fieldset>
  );
}

const num = (value: string): number | null =>
  value.trim() === "" ? null : Number(value);

const int = (value: string): number | null =>
  value.trim() === "" ? null : Math.trunc(Number(value));

const blank = (value: string): string | undefined =>
  value.trim() === "" ? undefined : value.trim();
