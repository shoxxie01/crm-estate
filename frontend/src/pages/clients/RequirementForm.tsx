import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react";
import { Check, ChevronDown, Plus, Star, X } from "lucide-react";
import {
  createRequirement,
  updateRequirement,
  type ClientDictionaries,
  type ClientRequirement,
  type RequirementPayload,
} from "../../api/clients";
import type { Dictionaries } from "../../api/properties";
import { ApiError } from "../../api/client";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { RangeField, fieldClass } from "../../components/ui/RangeField";
import { Select } from "../../components/ui/Select";
import { Textarea } from "../../components/ui/Textarea";
import { cn } from "../../lib/cn";
import { formatAmount, maskAmount, parseAmount } from "../../lib/amount";
import { FLOOR_TYPES, ROOMS_TYPES, appliesTo } from "./requirementMeta";

interface RequirementFormProps {
  clientId: string;
  /** Poszukiwanie do edycji; pusty = nowe. */
  initial: ClientRequirement | null;
  propertyDictionaries: Dictionaries;
  clientDictionaries: ClientDictionaries;
  onSaved: () => void;
  onClose: () => void;
}

type Importance = "required" | "preferred";

interface LocationRow {
  key: number;
  city: string;
  district: string;
}

const text = (value: number | null | undefined) =>
  value == null ? "" : String(value);

/** Przełącznik transakcji — czasownikiem, bo odpowiada na „Klient chce…". */
const TRANSACTION_CHOICES = [
  ["SALE", "Kupić"],
  ["RENT", "Najmować"],
] as const;

/** Metraż z przecinkiem jak w ofercie: `45` albo `45,5`. */
const AREA = /^\d{1,8}([.,]\d{1,2})?$/;

/**
 * Poszukiwanie — formularz pod rozmowę telefoniczną.
 *
 * Na górze to, o co agent pyta w pierwszej minucie (transakcja, rodzaj,
 * lokalizacja, budżet, metraż, pokoje). Reszta jest zwinięta, żeby nie
 * blokowała zapisu, gdy klient więcej nie powiedział. Wymagane są wyłącznie
 * transakcja i rodzaj nieruchomości.
 */
export function RequirementForm({
  clientId,
  initial,
  propertyDictionaries,
  clientDictionaries,
  onSaved,
  onClose,
}: RequirementFormProps) {
  const nextKey = useRef(0);
  const row = (city = "", district = ""): LocationRow => ({
    key: nextKey.current++,
    city,
    district,
  });

  const [transactionType, setTransactionType] = useState(
    initial?.transactionType ?? "SALE",
  );
  const [status, setStatus] = useState(initial?.status ?? "ACTIVE");
  const [propertyTypes, setPropertyTypes] = useState<Set<string>>(
    new Set(initial?.propertyTypes ?? ["APARTMENT"]),
  );
  const [locations, setLocations] = useState<LocationRow[]>(() =>
    initial?.locations.length
      ? initial.locations.map((l) => row(l.city, l.district ?? ""))
      : [row()],
  );
  const [fields, setFields] = useState({
    priceMin: formatAmount(text(initial?.priceMin)),
    priceMax: formatAmount(text(initial?.priceMax)),
    areaMin: text(initial?.areaMin).replace(".", ","),
    areaMax: text(initial?.areaMax).replace(".", ","),
    roomsMin: text(initial?.roomsMin),
    roomsMax: text(initial?.roomsMax),
    floorMin: text(initial?.floorMin),
    floorMax: text(initial?.floorMax),
    marketType: initial?.marketType ?? "",
    financing: initial?.financing ?? "",
    moveInDate: initial?.moveInDate ?? "",
    notes: initial?.notes ?? "",
  });
  const [excludeTopFloor, setExcludeTopFloor] = useState(
    initial?.excludeTopFloor ?? false,
  );
  const [features, setFeatures] = useState<Map<string, Importance>>(() => {
    const map = new Map<string, Importance>();
    initial?.preferredFeatures.forEach((f) => map.set(f, "preferred"));
    initial?.requiredFeatures.forEach((f) => map.set(f, "required"));
    return map;
  });

  // Przy edycji rozwijamy szczegóły, jeśli cokolwiek w nich jest — inaczej
  // zapisane dane byłyby niewidoczne i agent nie wiedziałby, że je ma.
  const [expanded, setExpanded] = useState(
    Boolean(
      initial &&
        (initial.marketType ||
          initial.floorMin != null ||
          initial.floorMax != null ||
          initial.excludeTopFloor ||
          initial.financing ||
          initial.moveInDate ||
          initial.requiredFeatures.length ||
          initial.preferredFeatures.length ||
          initial.notes),
    ),
  );

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const purchase = transactionType === "SALE";
  const showRooms = appliesTo(ROOMS_TYPES, propertyTypes);
  const showFloor = appliesTo(FLOOR_TYPES, propertyTypes);

  // Escape zamyka okno — jak w pozostałych dialogach aplikacji.
  const closeRef = useRef(onClose);
  closeRef.current = onClose;
  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") closeRef.current();
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  // Cechy pasujące do wybranych rodzajów; bez wyboru — wszystkie.
  const featureGroups = useMemo(
    () =>
      propertyDictionaries.featureGroups
        .map((group) => ({
          ...group,
          features: group.features.filter(
            (f) =>
              propertyTypes.size === 0 ||
              f.types.some((type) => propertyTypes.has(type)),
          ),
        }))
        .filter((group) => group.features.length > 0),
    [propertyDictionaries, propertyTypes],
  );

  const set = (name: keyof typeof fields) => (value: string) =>
    setFields((current) => ({ ...current, [name]: value }));

  // Kwoty grupowane w trakcie pisania. Jak przy telefonie: wartość i karetkę
  // ustawiamy na elemencie przed setState, żeby karetka nie uciekała na koniec.
  const setAmount =
    (name: "priceMin" | "priceMax") => (event: ChangeEvent<HTMLInputElement>) => {
      const input = event.target;
      const next = maskAmount(
        input.value,
        input.selectionStart ?? input.value.length,
        fields[name],
        (event.nativeEvent as InputEvent).inputType === "deleteContentForward",
      );
      input.value = next.value;
      input.setSelectionRange(next.caret, next.caret);
      set(name)(next.value);
    };

  const setInt =
    (name: "roomsMin" | "roomsMax" | "floorMin" | "floorMax", allowNegative = false) =>
    (event: ChangeEvent<HTMLInputElement>) => {
      const raw = event.target.value;
      const negative = allowNegative && raw.trimStart().startsWith("-");
      set(name)((negative ? "-" : "") + raw.replace(/\D/g, "").slice(0, 3));
    };

  const setArea =
    (name: "areaMin" | "areaMax") => (event: ChangeEvent<HTMLInputElement>) =>
      set(name)(event.target.value.replace(/[^\d.,]/g, ""));

  function switchTransaction(next: string) {
    setTransactionType(next);
    // Pokoju nie da się kupić — nie zostawiamy wyboru, którego serwer nie przyjmie.
    if (next === "SALE" && propertyTypes.has("ROOM")) {
      const without = new Set(propertyTypes);
      without.delete("ROOM");
      setPropertyTypes(without);
    }
  }

  function toggleType(value: string) {
    const next = new Set(propertyTypes);
    if (next.has(value)) next.delete(value);
    else next.add(value);
    setPropertyTypes(next);
  }

  // Jeden klik — musi mieć, drugi — mile widziane, trzeci — bez znaczenia.
  function cycleFeature(value: string) {
    const next = new Map(features);
    const current = next.get(value);
    if (!current) next.set(value, "required");
    else if (current === "required") next.set(value, "preferred");
    else next.delete(value);
    setFeatures(next);
  }

  function updateLocation(key: number, patch: Partial<LocationRow>) {
    setLocations((rows) =>
      rows.map((r) => (r.key === key ? { ...r, ...patch } : r)),
    );
  }

  function removeLocation(key: number) {
    setLocations((rows) => {
      const rest = rows.filter((r) => r.key !== key);
      return rest.length ? rest : [row()];
    });
  }

  /** Te same reguły co w serwisie, żeby błąd był widoczny przed wysłaniem. */
  function validate(): Record<string, string> {
    const found: Record<string, string> = {};
    const num = (value: string) =>
      value.trim() === "" ? null : Number(value.replace(",", "."));

    if (propertyTypes.size === 0) {
      found.propertyTypes = "Wybierz przynajmniej jeden rodzaj nieruchomości.";
    }

    locations.forEach((l, index) => {
      if (!l.city.trim() && l.district.trim()) {
        found[`locations[${index}].city`] = "Podaj miejscowość.";
      }
    });

    for (const key of ["areaMin", "areaMax"] as const) {
      if (fields[key] && !AREA.test(fields[key])) {
        found[key] = "Podaj liczbę, np. 45 lub 45,5.";
      }
    }
    for (const key of ["roomsMin", "roomsMax"] as const) {
      const value = num(fields[key]);
      if (showRooms && value != null && (value < 1 || value > 100)) {
        found[key] = "Od 1 do 100.";
      }
    }
    for (const key of ["floorMin", "floorMax"] as const) {
      const value = num(fields[key]);
      if (showFloor && value != null && (Number.isNaN(value) || value < -1 || value > 160)) {
        found[key] = "Od -1 (suterena) do 160.";
      }
    }

    const ranges: [string, number | null | undefined, number | null | undefined, string][] = [
      ["priceMax", parseAmount(fields.priceMin), parseAmount(fields.priceMax),
        "Budżet „do” nie może być niższy niż „od”."],
      ["areaMax", num(fields.areaMin), num(fields.areaMax),
        "Metraż „do” nie może być mniejszy niż „od”."],
      ["roomsMax", num(fields.roomsMin), num(fields.roomsMax),
        "Liczba pokoi „do” nie może być mniejsza niż „od”."],
      ["floorMax", num(fields.floorMin), num(fields.floorMax),
        "Piętro „do” nie może być niższe niż „od”."],
    ];
    for (const [key, min, max, text] of ranges) {
      if (!found[key] && min != null && max != null && min > max) {
        found[key] = text;
      }
    }

    return found;
  }

  async function submit(event: FormEvent) {
    event.preventDefault();

    const found = validate();
    if (Object.keys(found).length > 0) {
      setErrors(found);
      setMessage("Popraw zaznaczone pola.");
      return;
    }

    const optionalNumber = (value: string) =>
      value.trim() === "" || value === "-" ? undefined : Number(value.replace(",", "."));

    // Wysyłamy tylko cechy widoczne dla wybranych rodzajów — po zmianie
    // „mieszkanie" na „działkę" winda nie może zostać w poszukiwaniu po cichu.
    const visible = new Set(
      featureGroups.flatMap((group) => group.features.map((f) => f.value)),
    );
    const pick = (importance: Importance) =>
      [...features]
        .filter(([value, level]) => level === importance && visible.has(value))
        .map(([value]) => value);

    const payload: RequirementPayload = {
      transactionType,
      status: initial ? status : undefined,
      propertyTypes: [...propertyTypes],
      marketType: fields.marketType || undefined,
      locations: locations
        .filter((l) => l.city.trim())
        .map((l) => ({
          city: l.city.trim(),
          district: l.district.trim() || undefined,
        })),
      priceMin: parseAmount(fields.priceMin),
      priceMax: parseAmount(fields.priceMax),
      areaMin: optionalNumber(fields.areaMin),
      areaMax: optionalNumber(fields.areaMax),
      roomsMin: showRooms ? optionalNumber(fields.roomsMin) : undefined,
      roomsMax: showRooms ? optionalNumber(fields.roomsMax) : undefined,
      floorMin: showFloor ? optionalNumber(fields.floorMin) : undefined,
      floorMax: showFloor ? optionalNumber(fields.floorMax) : undefined,
      excludeTopFloor: showFloor && excludeTopFloor,
      financing: purchase ? fields.financing || undefined : undefined,
      moveInDate: fields.moveInDate || undefined,
      requiredFeatures: pick("required"),
      preferredFeatures: pick("preferred"),
      notes: fields.notes.trim() || undefined,
    };

    setSaving(true);
    setErrors({});
    setMessage(null);
    try {
      if (initial) {
        await updateRequirement(clientId, initial.id, payload);
      } else {
        await createRequirement(clientId, payload);
      }
      onSaved();
    } catch (cause) {
      if (cause instanceof ApiError) {
        setErrors(cause.fieldErrors ?? {});
        setMessage(cause.message);
      } else {
        setMessage("Nie udało się zapisać poszukiwania.");
      }
      setSaving(false);
    }
  }

  const propertyTypeOptions = propertyDictionaries.propertyType;

  return (
    <div
      className="fixed inset-0 z-40 flex items-start justify-center overflow-y-auto bg-ink/20 p-4 sm:p-8"
      role="dialog"
      aria-modal="true"
      aria-label={initial ? "Edycja poszukiwania" : "Nowe poszukiwanie"}
    >
      <form onSubmit={submit} className="card w-full max-w-2xl" noValidate>
        <header className="flex items-center justify-between border-b border-line px-4 py-3">
          <h2 className="text-[14px] font-semibold tracking-tight text-ink">
            {initial ? "Edytuj poszukiwanie" : "Nowe poszukiwanie"}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Zamknij"
            className="rounded p-1 text-ink-muted hover:bg-subtle hover:text-ink"
          >
            <X className="size-4" strokeWidth={2} />
          </button>
        </header>

        <div className="flex flex-col gap-5 p-4">
          {message && (
            <p className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
              {message}
            </p>
          )}

          <div className="grid gap-4 sm:grid-cols-2">
            <fieldset className="flex flex-col gap-1.5">
              <legend className="mb-1.5 text-[13px] font-medium text-ink">
                Klient chce<span className="ml-0.5 text-critical">*</span>
              </legend>
              <div className="inline-flex w-fit rounded-md border border-line bg-subtle p-0.5">
                {TRANSACTION_CHOICES.map(([value, label]) => (
                  <button
                    key={value}
                    type="button"
                    aria-pressed={transactionType === value}
                    onClick={() => switchTransaction(value)}
                    className={cn(
                      "h-8 rounded px-4 text-[13px] font-medium transition-colors",
                      transactionType === value
                        ? "bg-surface text-ink shadow-sm"
                        : "text-ink-secondary hover:text-ink",
                    )}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </fieldset>

            {initial && (
              <Select
                label="Stan"
                options={clientDictionaries.requirementStatus}
                value={status}
                onChange={(e) => setStatus(e.target.value)}
              />
            )}
          </div>

          <fieldset>
            <legend className="mb-1.5 text-[13px] font-medium text-ink">
              Rodzaj nieruchomości<span className="ml-0.5 text-critical">*</span>
            </legend>
            <div className="flex flex-wrap gap-1.5">
              {propertyTypeOptions.map((option) => {
                const disabled = purchase && option.value === "ROOM";
                const selected = propertyTypes.has(option.value);
                return (
                  <button
                    key={option.value}
                    type="button"
                    disabled={disabled}
                    aria-pressed={selected}
                    title={disabled ? "Pokój można tylko najmować." : undefined}
                    onClick={() => toggleType(option.value)}
                    className={cn(
                      "inline-flex h-8 items-center gap-1 rounded-md border px-2.5 text-[13px] transition-colors",
                      "disabled:cursor-not-allowed disabled:opacity-45",
                      selected
                        ? "border-accent bg-accent-subtle font-medium text-accent"
                        : "border-line bg-surface text-ink-secondary hover:border-line-strong hover:text-ink",
                    )}
                  >
                    {selected && <Check className="size-3.5" strokeWidth={2.25} />}
                    {option.label}
                  </button>
                );
              })}
            </div>
            {errors.propertyTypes && (
              <p className="mt-1.5 text-[12px] text-critical">{errors.propertyTypes}</p>
            )}
          </fieldset>

          <fieldset className="flex flex-col gap-2">
            <legend className="mb-1.5 text-[13px] font-medium text-ink">
              Lokalizacja
            </legend>
            {locations.map((location, index) => {
              const cityError = errors[`locations[${index}].city`];
              return (
                <div key={location.key} className="flex flex-col gap-1">
                  <div className="flex items-center gap-2">
                    <input
                      aria-label={`Miejscowość ${index + 1}`}
                      placeholder="Miejscowość"
                      value={location.city}
                      maxLength={80}
                      aria-invalid={cityError ? true : undefined}
                      onChange={(e) => updateLocation(location.key, { city: e.target.value })}
                      className={cn(fieldClass(Boolean(cityError)), "flex-1")}
                    />
                    <input
                      aria-label={`Dzielnica ${index + 1}`}
                      placeholder="Dzielnica (opcjonalnie)"
                      value={location.district}
                      maxLength={64}
                      onChange={(e) =>
                        updateLocation(location.key, { district: e.target.value })
                      }
                      className={cn(fieldClass(false), "flex-1")}
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      aria-label="Usuń lokalizację"
                      onClick={() => removeLocation(location.key)}
                      disabled={locations.length === 1 && !location.city && !location.district}
                    >
                      <X className="size-4" strokeWidth={2} />
                    </Button>
                  </div>
                  {cityError && <p className="text-[12px] text-critical">{cityError}</p>}
                </div>
              );
            })}
            {locations.length < 20 && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="w-fit"
                onClick={() => setLocations((rows) => [...rows, row()])}
              >
                <Plus className="size-4" strokeWidth={2} />
                Dodaj lokalizację
              </Button>
            )}
          </fieldset>

          <div className="grid gap-4 sm:grid-cols-3">
            <RangeField
              label={purchase ? "Budżet (zł)" : "Czynsz mies. (zł)"}
              min={fields.priceMin}
              max={fields.priceMax}
              onMin={setAmount("priceMin")}
              onMax={setAmount("priceMax")}
              inputMode="numeric"
              error={errors.priceMin ?? errors.priceMax}
            />
            <RangeField
              label="Metraż (m²)"
              min={fields.areaMin}
              max={fields.areaMax}
              onMin={setArea("areaMin")}
              onMax={setArea("areaMax")}
              inputMode="decimal"
              error={errors.areaMin ?? errors.areaMax}
            />
            {showRooms && (
              <RangeField
                label="Pokoje"
                min={fields.roomsMin}
                max={fields.roomsMax}
                onMin={setInt("roomsMin")}
                onMax={setInt("roomsMax")}
                inputMode="numeric"
                error={errors.roomsMin ?? errors.roomsMax}
              />
            )}
          </div>

          <div className="border-t border-line pt-4">
            <button
              type="button"
              onClick={() => setExpanded((v) => !v)}
              aria-expanded={expanded}
              className="flex items-center gap-1.5 text-[13px] font-medium text-ink-secondary hover:text-ink"
            >
              <ChevronDown
                className={cn("size-4 transition-transform", expanded && "rotate-180")}
                strokeWidth={2}
              />
              Więcej szczegółów
              <span className="font-normal text-ink-muted">
                — rynek, piętro, finansowanie, cechy, notatka
              </span>
            </button>

            {expanded && (
              <div className="mt-4 flex flex-col gap-5">
                <div className="grid gap-4 sm:grid-cols-3">
                  <Select
                    label="Rynek"
                    options={propertyDictionaries.marketType}
                    placeholder="Obojętny"
                    value={fields.marketType}
                    onChange={(e) => set("marketType")(e.target.value)}
                  />
                  {purchase && (
                    <Select
                      label="Finansowanie"
                      options={clientDictionaries.financing}
                      placeholder="Nie wiadomo"
                      value={fields.financing}
                      onChange={(e) => set("financing")(e.target.value)}
                    />
                  )}
                  <Input
                    label={purchase ? "Termin zakupu" : "Wprowadzenie od"}
                    type="date"
                    value={fields.moveInDate}
                    onChange={(e) => set("moveInDate")(e.target.value)}
                  />
                </div>

                {showFloor && (
                  <div className="grid items-end gap-4 sm:grid-cols-3">
                    <RangeField
                      label="Piętro"
                      hint="-1 suterena, 0 parter"
                      min={fields.floorMin}
                      max={fields.floorMax}
                      onMin={setInt("floorMin", true)}
                      onMax={setInt("floorMax", true)}
                      inputMode="numeric"
                      error={errors.floorMin ?? errors.floorMax}
                    />
                    <label className="flex h-9 items-center gap-2 text-[13px] text-ink sm:col-span-2">
                      <input
                        type="checkbox"
                        checked={excludeTopFloor}
                        onChange={(e) => setExcludeTopFloor(e.target.checked)}
                        className="size-4 rounded border-line accent-accent"
                      />
                      Bez ostatniego piętra
                    </label>
                  </div>
                )}

                {featureGroups.length > 0 && (
                  <fieldset className="flex flex-col gap-3">
                    <div>
                      <legend className="text-[13px] font-medium text-ink">Cechy</legend>
                      <p className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-[12px] text-ink-muted">
                        <span>Kliknięcie przełącza:</span>
                        <span className="inline-flex items-center gap-1">
                          <Check className="size-3.5 text-accent" strokeWidth={2.25} />
                          musi mieć
                        </span>
                        <span className="inline-flex items-center gap-1">
                          <Star className="size-3.5 text-accent" strokeWidth={2} />
                          mile widziane
                        </span>
                        <span>bez znaczenia</span>
                      </p>
                    </div>
                    {featureGroups.map((group) => (
                      <div key={group.category}>
                        <p className="mb-1.5 text-[11px] font-medium tracking-wide text-ink-muted uppercase">
                          {group.label}
                        </p>
                        <div className="flex flex-wrap gap-1.5">
                          {group.features.map((feature) => {
                            const level = features.get(feature.value);
                            return (
                              <button
                                key={feature.value}
                                type="button"
                                onClick={() => cycleFeature(feature.value)}
                                aria-label={`${feature.label}: ${
                                  level === "required"
                                    ? "musi mieć"
                                    : level === "preferred"
                                      ? "mile widziane"
                                      : "bez znaczenia"
                                }`}
                                className={cn(
                                  "inline-flex h-7 items-center gap-1 rounded-md border px-2 text-[12px] transition-colors",
                                  level === "required" &&
                                    "border-accent bg-accent-subtle font-medium text-accent",
                                  level === "preferred" &&
                                    "border-dashed border-accent/70 bg-surface text-ink",
                                  !level &&
                                    "border-line bg-surface text-ink-secondary hover:border-line-strong hover:text-ink",
                                )}
                              >
                                {level === "required" && (
                                  <Check className="size-3.5" strokeWidth={2.25} />
                                )}
                                {level === "preferred" && (
                                  <Star className="size-3.5 text-accent" strokeWidth={2} />
                                )}
                                {feature.label}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    ))}
                  </fieldset>
                )}

                <Textarea
                  label="Notatka"
                  rows={3}
                  value={fields.notes}
                  onChange={(e) => set("notes")(e.target.value)}
                  placeholder="Motywacja, na co zwraca uwagę, czego na pewno nie chce…"
                  error={errors.notes}
                />
              </div>
            )}
          </div>
        </div>

        <footer className="flex justify-end gap-2 border-t border-line px-4 py-3">
          <Button type="button" variant="secondary" onClick={onClose}>
            Anuluj
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? "Zapisywanie…" : "Zapisz poszukiwanie"}
          </Button>
        </footer>
      </form>
    </div>
  );
}
