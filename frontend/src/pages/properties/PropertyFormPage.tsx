import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Info } from "lucide-react";
import {
  createProperty,
  fetchDictionaries,
  fetchProperty,
  updateProperty,
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
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
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

  // `errors` trzyma wyłącznie błędy z backendu (ApiError). Błędy walidacji
  // klienta liczymy na żywo z fields — patrz liveErrors/errorFor niżej.
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [touched, setTouched] = useState<Set<string>>(new Set());
  const [submitted, setSubmitted] = useState(false);
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

  // Tryb edycji: wczytaj ofertę i rozłóż ją z powrotem na pola formularza.
  useEffect(() => {
    if (!id) return;
    fetchProperty(id)
      .then((p) => {
        setFields({
          propertyType: p.propertyType,
          transactionType: p.transactionType,
          marketType: p.marketType,
          status: p.status,
          title: p.title,
          description: p.description,
          price: str(p.pricing.price),
          priceCurrency: p.pricing.priceCurrency,
          rent: str(p.pricing.rent),
          deposit: str(p.pricing.deposit),
          commissionPercent: str(p.pricing.commissionPercent),
          totalArea: str(p.area.totalArea),
          usableArea: str(p.area.usableArea),
          plotArea: str(p.area.plotArea),
          roomsCount: str(p.area.roomsCount),
          bathroomsCount: str(p.area.bathroomsCount),
          floorNo: str(p.area.floorNo),
          buildingFloorsCount: str(p.area.buildingFloorsCount),
          ceilingHeight: str(p.area.ceilingHeight),
          voivodeship: p.address.voivodeship,
          county: p.address.county ?? "",
          commune: p.address.commune ?? "",
          city: p.address.city,
          district: p.address.district ?? "",
          street: p.address.street ?? "",
          buildingNumber: p.address.buildingNumber ?? "",
          postalCode: p.address.postalCode ?? "",
          buildYear: str(p.building.buildYear),
          buildingType: p.building.buildingType ?? "",
          buildingMaterial: p.building.buildingMaterial ?? "",
          constructionStatus: p.building.constructionStatus ?? "",
          ownershipForm: p.building.ownershipForm ?? "",
          windowsType: p.building.windowsType ?? "",
          surroundings: p.building.surroundings ?? "",
          plotType: p.land.plotType ?? "",
          plotDimensions: p.land.dimensions ?? "",
          roadAccess: p.land.roadAccess ?? "",
          zoningPlan: p.land.zoningPlan ?? "",
          hallStructure: p.commercial.structure ?? "",
          flooring: p.commercial.flooring ?? "",
          parkingType: p.commercial.parkingType ?? "",
          energyPrimary: str(p.energy.energyPrimary),
          energyFinal: str(p.energy.energyFinal),
          energyClass: p.energy.energyClass ?? "",
          energyCertNumber: p.energy.certificateNumber ?? "",
          availableFrom: p.availableFrom ?? "",
          videoUrl: p.videoUrl ?? "",
          panoramaUrl: p.panoramaUrl ?? "",
          keysInfo: p.keysInfo ?? "",
          privateNotes: p.privateNotes ?? "",
          energyExemptNote: p.energy.exemptNote ?? "",
        });
        setFeatures(new Set(p.features));
        setHeating(new Set(p.heatingTypes));
        setUses(new Set(p.commercialUses));
        setFlags({
          priceNegotiable: p.pricing.priceNegotiable,
          priceIncludesRent: p.pricing.priceIncludesRent,
          hideExactAddress: p.address.hideExactAddress,
          furnished: p.building.furnished ?? false,
          plotFenced: p.land.fenced ?? false,
          officeSpace: p.commercial.officeSpace ?? false,
          socialFacilities: p.commercial.socialFacilities ?? false,
          loadingRamp: p.commercial.loadingRamp ?? false,
          energyExempt: p.energy.exempt,
          exportable: p.exportable,
        });
      })
      .catch(() => setFormError("Nie udało się wczytać oferty do edycji."));
  }, [id]);

  const set = (name: string) => (event: { target: { value: string } }) =>
    setFields((current) => ({ ...current, [name]: event.target.value }));

  // Oznacza pole jako „dotknięte" (opuszczone) — od tej chwili jego błąd jest
  // widoczny i aktualizuje się na żywo przy każdej zmianie.
  const markTouched = (key: string) => () =>
    setTouched((current) =>
      current.has(key) ? current : new Set(current).add(key),
    );

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

  // Walidacja realnych zakresów — Bean Validation na backendzie pilnuje reszty,
  // ale sensowne granice („piętro do 154", „rok budowy nie z przyszłości") lepiej
  // pokazać od razu przy polu, zanim żądanie w ogóle poleci.
  function validate(): Record<string, string> {
    const e: Record<string, string> = {};
    const n = (v: string): number | null => {
      const t = v.trim().replace(",", ".");
      return t === "" ? null : Number(t);
    };
    // Reguła „maks. 2 miejsca po przecinku": łapie 3+ cyfry po przecinku/kropce.
    const tooManyDecimals = (v: string): boolean => /[.,]\d{3,}/.test(v.trim());
    const maxYear = new Date().getFullYear() + 10;

    const price = n(fields.price);
    if (price == null || Number.isNaN(price) || price <= 0)
      e["pricing.price"] = "Podaj cenę większą od zera.";
    else if (price > 9_999_999_999)
      e["pricing.price"] = "Cena jest nierealnie wysoka.";

    const rent = n(fields.rent);
    if (rent != null && (Number.isNaN(rent) || rent < 0))
      e["pricing.rent"] = "Czynsz nie może być ujemny.";
    else if (rent != null && rent > 1_000_000)
      e["pricing.rent"] = "Czynsz jest nierealnie wysoki.";

    const deposit = n(fields.deposit);
    if (deposit != null && (Number.isNaN(deposit) || deposit < 0))
      e["pricing.deposit"] = "Kaucja nie może być ujemna.";

    const commission = n(fields.commissionPercent);
    if (commission != null && (Number.isNaN(commission) || commission < 0 || commission > 100))
      e["pricing.commissionPercent"] = "Prowizja musi być w zakresie 0–100%.";

    const total = n(fields.totalArea);
    if (total == null || Number.isNaN(total) || total <= 0)
      e["area.totalArea"] = "Podaj powierzchnię większą od zera.";
    else if (total > 1_000_000)
      e["area.totalArea"] = "Powierzchnia jest nierealnie duża.";

    const usable = n(fields.usableArea);
    if (usable != null && (Number.isNaN(usable) || usable <= 0))
      e["area.usableArea"] = "Powierzchnia użytkowa musi być większa od zera.";
    else if (usable != null && total != null && usable > total)
      e["area.usableArea"] = "Powierzchnia użytkowa nie może przekraczać całkowitej.";

    const plot = n(fields.plotArea);
    if (plot != null && (Number.isNaN(plot) || plot <= 0))
      e["area.plotArea"] = "Powierzchnia działki musi być większa od zera.";

    const rooms = n(fields.roomsCount);
    if (rooms != null && (!Number.isInteger(rooms) || rooms < 1 || rooms > 100))
      e["area.roomsCount"] = "Liczba pokoi musi być z zakresu 1–100.";

    const baths = n(fields.bathroomsCount);
    if (baths != null && (!Number.isInteger(baths) || baths < 0 || baths > 50))
      e["area.bathroomsCount"] = "Liczba łazienek musi być z zakresu 0–50.";

    const floor = n(fields.floorNo);
    if (floor != null && (!Number.isInteger(floor) || floor < -1 || floor > 160))
      e["area.floorNo"] = "Piętro musi być z zakresu od -1 (suterena) do 160.";

    const buildingFloors = n(fields.buildingFloorsCount);
    if (buildingFloors != null && (!Number.isInteger(buildingFloors) || buildingFloors < 1 || buildingFloors > 200))
      e["area.buildingFloorsCount"] = "Liczba pięter musi być z zakresu 1–200.";

    if (
      floor != null && buildingFloors != null &&
      Number.isInteger(floor) && Number.isInteger(buildingFloors) &&
      floor > buildingFloors
    )
      e["area.floorNo"] = "Piętro nie może być wyższe niż liczba pięter w budynku.";

    const ceiling = n(fields.ceilingHeight);
    if (ceiling != null && (Number.isNaN(ceiling) || ceiling < 1 || ceiling > 50))
      e["area.ceilingHeight"] = "Wysokość pomieszczeń musi być z zakresu 1–50 m.";

    const year = n(fields.buildYear);
    if (year != null && (!Number.isInteger(year) || year < 1800 || year > maxYear))
      e["building.buildYear"] = `Rok budowy musi być z zakresu 1800–${maxYear}.`;

    const ep = n(fields.energyPrimary);
    if (ep != null && (Number.isNaN(ep) || ep < 0 || ep > 5000))
      e["energy.energyPrimary"] = "Wskaźnik EP jest poza realnym zakresem (0–5000).";

    const ek = n(fields.energyFinal);
    if (ek != null && (Number.isNaN(ek) || ek < 0 || ek > 5000))
      e["energy.energyFinal"] = "Wskaźnik EK jest poza realnym zakresem (0–5000).";

    const postal = fields.postalCode.trim();
    if (postal !== "" && !/^\d{2}-\d{3}$/.test(postal))
      e["address.postalCode"] = "Kod pocztowy w formacie 00-000.";

    // Wszystkie pola dziesiętne: maksymalnie 2 miejsca po przecinku. Sprawdzamy
    // na końcu, więc ten komunikat wygrywa z ewentualnym błędem zakresu.
    const decimalFields: [string, string][] = [
      ["price", "pricing.price"],
      ["rent", "pricing.rent"],
      ["deposit", "pricing.deposit"],
      ["commissionPercent", "pricing.commissionPercent"],
      ["totalArea", "area.totalArea"],
      ["usableArea", "area.usableArea"],
      ["plotArea", "area.plotArea"],
      ["ceilingHeight", "area.ceilingHeight"],
      ["energyPrimary", "energy.energyPrimary"],
      ["energyFinal", "energy.energyFinal"],
    ];
    for (const [field, key] of decimalFields) {
      if (tooManyDecimals(fields[field]))
        e[key] = "Maksymalnie 2 miejsca po przecinku.";
    }

    return e;
  }

  // Wynik walidacji klienta liczony przy każdym renderze — zawsze świeży.
  const liveErrors = validate();

  // Błąd pokazujemy, gdy: backend go zwrócił, albo formularz był już wysłany,
  // albo użytkownik opuścił to pole. Dzięki temu komunikat pojawia się od razu,
  // a nie dopiero po kliknięciu „Zapisz".
  const errorFor = (key: string): string | undefined =>
    errors[key] ??
    (submitted || touched.has(key) ? liveErrors[key] : undefined);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setSubmitted(true);
    setErrors({});
    setFormError(null);

    const fieldErrors = validate();
    if (Object.keys(fieldErrors).length > 0) {
      setFormError("Popraw zaznaczone pola formularza.");
      return;
    }

    setSaving(true);

    try {
      const payload = {
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
      };

      const saved =
        isEdit && id
          ? await updateProperty(id, payload)
          : await createProperty(payload);

      navigate("/nieruchomosci", { state: { saved: saved.id } });
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
            {isEdit ? "Edytuj ofertę" : "Nowa oferta"}
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
          inputMode="decimal"
          step="0.01"
          min={0}
          value={fields.price}
          onChange={set("price")}
          onBlur={markTouched("pricing.price")}
          error={errorFor("pricing.price")}
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
          inputMode="decimal"
          step="0.01"
          min={0}
          max={100}
          value={fields.commissionPercent}
          onChange={set("commissionPercent")}
          onBlur={markTouched("pricing.commissionPercent")}
          error={errorFor("pricing.commissionPercent")}
          hint="Nie trafia do ogłoszenia."
        />
        <Input
          label="Czynsz administracyjny"
          type="number"
          inputMode="decimal"
          step="0.01"
          min={0}
          value={fields.rent}
          onChange={set("rent")}
          onBlur={markTouched("pricing.rent")}
          error={errorFor("pricing.rent")}
        />
        {isRent && (
          <Input
            label="Kaucja"
            type="number"
            inputMode="decimal"
            step="0.01"
            min={0}
            value={fields.deposit}
            onChange={set("deposit")}
            onBlur={markTouched("pricing.deposit")}
            error={errorFor("pricing.deposit")}
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
          inputMode="decimal"
          step="0.01"
          min={0}
          value={fields.totalArea}
          onChange={set("totalArea")}
          onBlur={markTouched("area.totalArea")}
          error={errorFor("area.totalArea")}
          required
        />
        <Input
          label="Powierzchnia użytkowa (m²)"
          type="number"
          inputMode="decimal"
          step="0.01"
          min={0}
          value={fields.usableArea}
          onChange={set("usableArea")}
          onBlur={markTouched("area.usableArea")}
          error={errorFor("area.usableArea")}
        />
        {showLand && (
          <Input
            label="Powierzchnia działki (m²)"
            type="number"
            inputMode="decimal"
            step="0.01"
            min={0}
            value={fields.plotArea}
            onChange={set("plotArea")}
            onBlur={markTouched("area.plotArea")}
            error={errorFor("area.plotArea")}
          />
        )}
        {!isLand && (
          <>
            <Input
              label={`Liczba pokoi${roomsRequired ? "" : " (opcjonalnie)"}`}
              type="number"
              inputMode="numeric"
              step="1"
              min={1}
              max={100}
              value={fields.roomsCount}
              onChange={set("roomsCount")}
              onBlur={markTouched("area.roomsCount")}
              error={errorFor("area.roomsCount")}
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
              inputMode="numeric"
              step="1"
              min={0}
              max={50}
              value={fields.bathroomsCount}
              onChange={set("bathroomsCount")}
              onBlur={markTouched("area.bathroomsCount")}
              error={errorFor("area.bathroomsCount")}
            />
            <Input
              label="Piętro"
              type="number"
              inputMode="numeric"
              step="1"
              min={-1}
              max={160}
              value={fields.floorNo}
              onChange={set("floorNo")}
              onBlur={markTouched("area.floorNo")}
              error={errorFor("area.floorNo")}
              hint="-1 = suterena, 0 = parter."
            />
            <Input
              label="Liczba pięter w budynku"
              type="number"
              inputMode="numeric"
              step="1"
              min={1}
              max={200}
              value={fields.buildingFloorsCount}
              onChange={set("buildingFloorsCount")}
              onBlur={markTouched("area.buildingFloorsCount")}
              error={errorFor("area.buildingFloorsCount")}
            />
          </>
        )}
        {isCommercial && (
          <Input
            label="Wysokość pomieszczeń (m)"
            type="number"
            inputMode="decimal"
            step="0.01"
            min={1}
            max={50}
            value={fields.ceilingHeight}
            onChange={set("ceilingHeight")}
            onBlur={markTouched("area.ceilingHeight")}
            error={errorFor("area.ceilingHeight")}
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
          onBlur={markTouched("address.postalCode")}
          error={errorFor("address.postalCode")}
          placeholder="00-000"
          inputMode="numeric"
          maxLength={6}
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
            inputMode="numeric"
            step="1"
            min={1800}
            max={new Date().getFullYear() + 10}
            value={fields.buildYear}
            onChange={set("buildYear")}
            onBlur={markTouched("building.buildYear")}
            error={errorFor("building.buildYear")}
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
              inputMode="decimal"
              step="0.01"
              min={0}
              value={fields.energyPrimary}
              onChange={set("energyPrimary")}
              onBlur={markTouched("energy.energyPrimary")}
              error={errorFor("energy.energyPrimary")}
              hint="kWh/(m²·rok)"
            />
            <Input
              label="EK — energia końcowa"
              type="number"
              inputMode="decimal"
              step="0.01"
              min={0}
              value={fields.energyFinal}
              onChange={set("energyFinal")}
              onBlur={markTouched("energy.energyFinal")}
              error={errorFor("energy.energyFinal")}
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

// Przecinek jako separator dziesiętny (polska konwencja) sprowadzamy do kropki
// przed parsowaniem — inaczej Number("2,5") to NaN.
const num = (value: string): number | null => {
  const trimmed = value.trim().replace(",", ".");
  return trimmed === "" ? null : Number(trimmed);
};

const int = (value: string): number | null => {
  const trimmed = value.trim().replace(",", ".");
  return trimmed === "" ? null : Math.trunc(Number(trimmed));
};

const blank = (value: string): string | undefined =>
  value.trim() === "" ? undefined : value.trim();

const str = (value: number | null): string =>
  value == null ? "" : String(value);
