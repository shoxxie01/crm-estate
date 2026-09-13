import { useEffect, useRef, useState, type ChangeEvent, type FormEvent, type ReactNode } from "react";
import { useParams } from "react-router-dom";
import { Check, CheckCircle2, Plus, X } from "lucide-react";
import {
  fetchIntakeForm,
  submitInquiry,
  type InquiryIntent,
  type IntakeForm,
} from "../../api/inquiries";
import { ApiError } from "../../api/client";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { PhoneInput } from "../../components/ui/PhoneInput";
import { RangeField, fieldClass } from "../../components/ui/RangeField";
import { Select } from "../../components/ui/Select";
import { Textarea } from "../../components/ui/Textarea";
import { cn } from "../../lib/cn";
import { maskAmount, parseAmount } from "../../lib/amount";
import { phoneError, phoneToPayload } from "../../lib/phone";
import { ROOMS_TYPES, appliesTo } from "../clients/requirementMeta";

const MAX_LOCATIONS = 5;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Tylko dwie opcje — najem obsługuje agent przy rozmowie. */
const INTENTS: [InquiryIntent, string][] = [
  ["BUY", "Kupić"],
  ["SELL", "Sprzedać"],
];

/** Pokoju nie da się ani kupić, ani sprzedać osobno — portale przyjmują go tylko na najem. */
const withoutRooms = (types: IntakeForm["propertyType"]) =>
  types.filter((type) => type.value !== "ROOM");

interface LocationRow {
  key: number;
  city: string;
  district: string;
}

type AmountSetter = (event: ChangeEvent<HTMLInputElement>) => void;

/**
 * Publiczny formularz zgłoszeniowy — strona bez logowania, pod linkiem biura.
 * Pisany dla klienta, nie dla agenta: dwie ścieżki („kupić" / „sprzedać"),
 * bez żargonu i bez pól, o które klient i tak nie umiałby odpowiedzieć.
 */
export function IntakePage() {
  const { token = "" } = useParams<{ token: string }>();
  const [form, setForm] = useState<IntakeForm | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [sent, setSent] = useState<InquiryIntent | null>(null);

  useEffect(() => {
    fetchIntakeForm(token)
      .then(setForm)
      .catch((cause) =>
        setLoadError(
          cause instanceof ApiError && cause.status !== 0
            ? cause.message
            : "Nie udało się wczytać formularza. Spróbuj odświeżyć stronę.",
        ),
      );
  }, [token]);

  return (
    <div className="min-h-dvh bg-canvas px-4 py-8 sm:py-12">
      <div className="mx-auto w-full max-w-2xl">
        {loadError ? (
          <Notice title="Formularz jest niedostępny" body={loadError} />
        ) : !form ? (
          <p className="text-center text-[13px] text-ink-muted">Wczytywanie…</p>
        ) : sent ? (
          <Notice
            success
            title="Dziękujemy, zgłoszenie dotarło"
            body={
              sent === "SELL"
                ? `${form.agencyName} odezwie się, żeby porozmawiać o sprzedaży i umówić się na oględziny.`
                : `${form.agencyName} odezwie się, gdy tylko przejrzy Twoje kryteria — zwykle z pierwszymi pasującymi ofertami.`
            }
          />
        ) : (
          <IntakeFormView token={token} form={form} onSent={setSent} />
        )}

        <p className="mt-6 text-center text-[11px] text-ink-muted">
          Formularz obsługuje Delta CRM
        </p>
      </div>
    </div>
  );
}

function Notice({ title, body, success }: { title: string; body: string; success?: boolean }) {
  return (
    <div className="card px-6 py-10 text-center">
      {success && (
        <CheckCircle2 className="mx-auto mb-3 size-8 text-good" strokeWidth={1.75} />
      )}
      <h1 className="text-lg font-semibold tracking-tight text-ink">{title}</h1>
      <p className="mx-auto mt-2 max-w-md text-[13px] leading-relaxed text-ink-secondary">
        {body}
      </p>
    </div>
  );
}

function IntakeFormView({
  token,
  form,
  onSent,
}: {
  token: string;
  form: IntakeForm;
  onSent: (intent: InquiryIntent) => void;
}) {
  const nextKey = useRef(1);
  const propertyTypes = withoutRooms(form.propertyType);

  const [intent, setIntent] = useState<InquiryIntent>("BUY");
  const [contact, setContact] = useState({ firstName: "", lastName: "", phone: "", email: "" });

  // --- kupno ---
  const [wantedTypes, setWantedTypes] = useState<Set<string>>(new Set(["APARTMENT"]));
  const [locations, setLocations] = useState<LocationRow[]>([{ key: 0, city: "", district: "" }]);
  const [criteria, setCriteria] = useState({
    priceMin: "",
    priceMax: "",
    areaMin: "",
    areaMax: "",
    roomsMin: "",
    roomsMax: "",
    financing: "",
    moveInDate: "",
  });

  // --- sprzedaż ---
  const [offer, setOffer] = useState({
    propertyType: "APARTMENT",
    city: "",
    district: "",
    area: "",
    roomsCount: "",
    expectedPrice: "",
  });

  const [message, setMessage] = useState("");
  const [consentProcessing, setConsentProcessing] = useState(false);
  const [consentMarketing, setConsentMarketing] = useState(false);
  const [website, setWebsite] = useState("");

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
  const [errorSignal, setErrorSignal] = useState(0);

  const buying = intent === "BUY";
  const showWantedRooms = appliesTo(ROOMS_TYPES, wantedTypes);
  const showOfferRooms = ROOMS_TYPES.has(offer.propertyType);

  // Po nieudanej próbie — do pierwszego błędnego pola, jak w formularzach CRM.
  useEffect(() => {
    if (errorSignal === 0) return;
    const invalid = document.querySelector<HTMLElement>('[aria-invalid="true"]');
    invalid?.scrollIntoView({ behavior: "smooth", block: "center" });
    invalid?.focus({ preventScroll: true });
  }, [errorSignal]);

  /** Kwota grupowana spacjami w trakcie pisania — karetka zostaje na miejscu. */
  const amountSetter =
    (previous: string, apply: (value: string) => void): AmountSetter =>
    (event) => {
      const input = event.target;
      const next = maskAmount(
        input.value,
        input.selectionStart ?? input.value.length,
        previous,
        (event.nativeEvent as InputEvent).inputType === "deleteContentForward",
      );
      input.value = next.value;
      input.setSelectionRange(next.caret, next.caret);
      apply(next.value);
    };

  const digits = (value: string, max: number) => value.replace(/\D/g, "").slice(0, max);
  const decimal = (value: string) => value.replace(/[^\d,]/g, "").slice(0, 8);
  const toNumber = (value: string) => (value ? Number(value.replace(",", ".")) : undefined);

  function toggleType(value: string) {
    const next = new Set(wantedTypes);
    if (next.has(value)) next.delete(value);
    else next.add(value);
    setWantedTypes(next);
  }

  function switchIntent(next: InquiryIntent) {
    setIntent(next);
    setErrors({});
    setFormError(null);
  }

  function validate(): Record<string, string> {
    const found: Record<string, string> = {};

    if (!contact.firstName.trim()) found.firstName = "Podaj imię.";
    if (!contact.lastName.trim()) found.lastName = "Podaj nazwisko.";
    const phoneProblem = phoneError(contact.phone);
    if (phoneProblem) found.phone = phoneProblem;
    if (contact.email.trim() && !EMAIL.test(contact.email.trim())) {
      found.email = "Podaj poprawny adres e-mail.";
    }
    if (!found.phone && !contact.phone.trim() && !contact.email.trim()) {
      found.phone = "Podaj telefon lub e-mail — inaczej biuro nie będzie mogło się z Tobą skontaktować.";
    }

    if (buying) {
      if (wantedTypes.size === 0) {
        found["criteria.propertyTypes"] = "Wybierz przynajmniej jeden rodzaj nieruchomości.";
      }
      locations.forEach((l, index) => {
        if (!l.city.trim() && l.district.trim()) {
          found[`criteria.locations[${index}].city`] = "Podaj miejscowość.";
        }
      });
      const pairs: [string, number | undefined, number | undefined, string][] = [
        ["criteria.priceMax", parseAmount(criteria.priceMin), parseAmount(criteria.priceMax),
          "Kwota „do” nie może być niższa niż „od”."],
        ["criteria.areaMax", toNumber(criteria.areaMin), toNumber(criteria.areaMax),
          "Metraż „do” nie może być mniejszy niż „od”."],
        ["criteria.roomsMax", toNumber(criteria.roomsMin), toNumber(criteria.roomsMax),
          "Liczba pokoi „do” nie może być mniejsza niż „od”."],
      ];
      for (const [key, min, max, text] of pairs) {
        if (min != null && max != null && min > max) found[key] = text;
      }
      if (showWantedRooms && (criteria.roomsMin === "0" || criteria.roomsMax === "0")) {
        found["criteria.roomsMax"] = "Liczba pokoi musi być dodatnia.";
      }
    } else {
      if (!offer.city.trim()) found["offer.city"] = "Podaj miejscowość.";
      if (offer.area && !(toNumber(offer.area)! > 0)) {
        found["offer.area"] = "Powierzchnia musi być większa od zera.";
      }
      if (showOfferRooms && offer.roomsCount === "0") {
        found["offer.roomsCount"] = "Liczba pokoi musi być dodatnia.";
      }
    }

    if (!consentProcessing) {
      found.consentProcessing = "Bez tej zgody biuro nie może odpowiedzieć na zgłoszenie.";
    }
    return found;
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);

    const found = validate();
    if (Object.keys(found).length > 0) {
      setErrors(found);
      setFormError("Popraw zaznaczone pola.");
      setErrorSignal((s) => s + 1);
      return;
    }

    setSending(true);
    setErrors({});
    try {
      await submitInquiry(token, {
        intent,
        firstName: contact.firstName.trim(),
        lastName: contact.lastName.trim(),
        phone: phoneToPayload(contact.phone),
        email: contact.email.trim() || undefined,
        criteria: buying
          ? {
              transactionType: "SALE",
              propertyTypes: [...wantedTypes],
              locations: locations
                .filter((l) => l.city.trim())
                .map((l) => ({ city: l.city.trim(), district: l.district.trim() || undefined })),
              priceMin: parseAmount(criteria.priceMin),
              priceMax: parseAmount(criteria.priceMax),
              areaMin: toNumber(criteria.areaMin),
              areaMax: toNumber(criteria.areaMax),
              roomsMin: showWantedRooms ? toNumber(criteria.roomsMin) : undefined,
              roomsMax: showWantedRooms ? toNumber(criteria.roomsMax) : undefined,
              excludeTopFloor: false,
              financing: criteria.financing || undefined,
              moveInDate: criteria.moveInDate || undefined,
              requiredFeatures: [],
              preferredFeatures: [],
            }
          : undefined,
        offer: buying
          ? undefined
          : {
              propertyType: offer.propertyType,
              city: offer.city.trim(),
              district: offer.district.trim() || undefined,
              area: toNumber(offer.area),
              roomsCount: showOfferRooms ? toNumber(offer.roomsCount) : undefined,
              expectedPrice: parseAmount(offer.expectedPrice),
            },
        message: message.trim() || undefined,
        consentProcessing,
        consentMarketing,
        website,
      });
      onSent(intent);
    } catch (cause) {
      if (cause instanceof ApiError) {
        setErrors(cause.fieldErrors ?? {});
        setFormError(cause.message);
        setErrorSignal((s) => s + 1);
      } else {
        setFormError("Nie udało się wysłać zgłoszenia. Spróbuj ponownie.");
      }
      setSending(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="card overflow-hidden">
      <header className="border-b border-line px-5 py-5 sm:px-6">
        <p className="text-[12px] font-medium tracking-wide text-accent uppercase">
          {form.agencyName}
        </p>
        <h1 className="mt-1 text-xl font-semibold tracking-tight text-ink">
          Jak możemy pomóc?
        </h1>
        <p className="mt-1 text-[13px] leading-relaxed text-ink-secondary">
          Kilka pytań zamiast wielu telefonów. Wypełnij tylko to, co wiesz —
          resztę ustalimy w rozmowie.
        </p>
      </header>

      <div className="flex flex-col gap-7 px-5 py-6 sm:px-6">
        {formError && (
          <p
            role="alert"
            className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical"
          >
            {formError}
          </p>
        )}

        <fieldset>
          <legend className="mb-2 text-[13px] font-semibold tracking-tight text-ink">Chcę</legend>
          <div className="grid grid-cols-2 gap-2 sm:max-w-sm">
            {INTENTS.map(([value, label]) => (
              <button
                key={value}
                type="button"
                aria-pressed={intent === value}
                onClick={() => switchIntent(value)}
                className={cn(
                  "flex h-11 items-center justify-center gap-1.5 rounded-md border text-[14px] font-medium transition-colors",
                  intent === value
                    ? "border-accent bg-accent-subtle text-accent"
                    : "border-line bg-surface text-ink-secondary hover:border-line-strong hover:text-ink",
                )}
              >
                {intent === value && <Check className="size-4" strokeWidth={2.25} />}
                {label}
              </button>
            ))}
          </div>
        </fieldset>

        {buying ? (
          <Group title="Czego szukasz">
            <fieldset className="sm:col-span-2">
              <legend className="mb-1.5 text-[13px] font-medium text-ink">
                Rodzaj nieruchomości<span className="ml-0.5 text-critical">*</span>
              </legend>
              <div className="flex flex-wrap gap-1.5">
                {propertyTypes.map((option) => {
                  const selected = wantedTypes.has(option.value);
                  return (
                    <button
                      key={option.value}
                      type="button"
                      aria-pressed={selected}
                      onClick={() => toggleType(option.value)}
                      className={cn(
                        "inline-flex h-8 items-center gap-1 rounded-md border px-2.5 text-[13px] transition-colors",
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
              {errors["criteria.propertyTypes"] && (
                <p className="mt-1.5 text-[12px] text-critical">{errors["criteria.propertyTypes"]}</p>
              )}
            </fieldset>

            <fieldset className="flex flex-col gap-2 sm:col-span-2">
              <legend className="mb-1.5 text-[13px] font-medium text-ink">Gdzie</legend>
              {locations.map((location, index) => {
                const cityError = errors[`criteria.locations[${index}].city`];
                const update = (patch: Partial<LocationRow>) =>
                  setLocations((rows) =>
                    rows.map((r) => (r.key === location.key ? { ...r, ...patch } : r)),
                  );
                return (
                  <div key={location.key} className="flex flex-col gap-1">
                    <div className="flex items-center gap-2">
                      <input
                        aria-label={`Miejscowość ${index + 1}`}
                        placeholder="Miejscowość"
                        value={location.city}
                        maxLength={80}
                        aria-invalid={cityError ? true : undefined}
                        onChange={(e) => update({ city: e.target.value })}
                        className={cn(fieldClass(Boolean(cityError)), "flex-1")}
                      />
                      <input
                        aria-label={`Dzielnica ${index + 1}`}
                        placeholder="Dzielnica (opcjonalnie)"
                        value={location.district}
                        maxLength={64}
                        onChange={(e) => update({ district: e.target.value })}
                        className={cn(fieldClass(false), "flex-1")}
                      />
                      {locations.length > 1 && (
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          aria-label="Usuń lokalizację"
                          onClick={() => setLocations((rows) => rows.filter((r) => r.key !== location.key))}
                        >
                          <X className="size-4" strokeWidth={2} />
                        </Button>
                      )}
                    </div>
                    {cityError && <p className="text-[12px] text-critical">{cityError}</p>}
                  </div>
                );
              })}
              {locations.length < MAX_LOCATIONS && (
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="w-fit"
                  onClick={() =>
                    setLocations((rows) => [...rows, { key: nextKey.current++, city: "", district: "" }])
                  }
                >
                  <Plus className="size-4" strokeWidth={2} />
                  Dodaj kolejną lokalizację
                </Button>
              )}
            </fieldset>

            <RangeField
              label="Budżet (zł)"
              min={criteria.priceMin}
              max={criteria.priceMax}
              onMin={amountSetter(criteria.priceMin, (v) => setCriteria((c) => ({ ...c, priceMin: v })))}
              onMax={amountSetter(criteria.priceMax, (v) => setCriteria((c) => ({ ...c, priceMax: v })))}
              inputMode="numeric"
              error={errors["criteria.priceMin"] ?? errors["criteria.priceMax"]}
            />
            <RangeField
              label="Metraż (m²)"
              min={criteria.areaMin}
              max={criteria.areaMax}
              onMin={(e) => setCriteria((c) => ({ ...c, areaMin: decimal(e.target.value) }))}
              onMax={(e) => setCriteria((c) => ({ ...c, areaMax: decimal(e.target.value) }))}
              inputMode="decimal"
              error={errors["criteria.areaMin"] ?? errors["criteria.areaMax"]}
            />
            {showWantedRooms && (
              <RangeField
                label="Liczba pokoi"
                min={criteria.roomsMin}
                max={criteria.roomsMax}
                onMin={(e) => setCriteria((c) => ({ ...c, roomsMin: digits(e.target.value, 2) }))}
                onMax={(e) => setCriteria((c) => ({ ...c, roomsMax: digits(e.target.value, 2) }))}
                inputMode="numeric"
                error={errors["criteria.roomsMin"] ?? errors["criteria.roomsMax"]}
              />
            )}
            <Select
              label="Jak planujesz sfinansować zakup?"
              options={form.financing}
              placeholder="Jeszcze nie wiem"
              value={criteria.financing}
              onChange={(e) => setCriteria((c) => ({ ...c, financing: e.target.value }))}
            />
            <Input
              label="Do kiedy chcesz kupić?"
              type="date"
              value={criteria.moveInDate}
              onChange={(e) => setCriteria((c) => ({ ...c, moveInDate: e.target.value }))}
            />
          </Group>
        ) : (
          <Group title="Co chcesz sprzedać">
            <div className="sm:col-span-2">
              <Select
                label="Rodzaj nieruchomości"
                required
                options={propertyTypes}
                value={offer.propertyType}
                onChange={(e) => setOffer((o) => ({ ...o, propertyType: e.target.value }))}
                error={errors["offer.propertyType"]}
              />
            </div>
            <Input
              label="Miejscowość"
              required
              maxLength={80}
              value={offer.city}
              onChange={(e) => setOffer((o) => ({ ...o, city: e.target.value }))}
              error={errors["offer.city"]}
            />
            <Input
              label="Dzielnica"
              maxLength={64}
              value={offer.district}
              onChange={(e) => setOffer((o) => ({ ...o, district: e.target.value }))}
              error={errors["offer.district"]}
            />
            <Input
              label="Powierzchnia (m²)"
              inputMode="decimal"
              value={offer.area}
              onChange={(e) => setOffer((o) => ({ ...o, area: decimal(e.target.value) }))}
              error={errors["offer.area"]}
            />
            {showOfferRooms && (
              <Input
                label="Liczba pokoi"
                inputMode="numeric"
                value={offer.roomsCount}
                onChange={(e) => setOffer((o) => ({ ...o, roomsCount: digits(e.target.value, 2) }))}
                error={errors["offer.roomsCount"]}
              />
            )}
            <Input
              label="Oczekiwana cena (zł)"
              inputMode="numeric"
              value={offer.expectedPrice}
              onChange={amountSetter(offer.expectedPrice, (v) => setOffer((o) => ({ ...o, expectedPrice: v })))}
              hint="Jeśli nie wiesz — zostaw puste, pomożemy wycenić."
              error={errors["offer.expectedPrice"]}
            />
          </Group>
        )}

        <Textarea
          label="Coś jeszcze?"
          rows={3}
          maxLength={2000}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder={
            buying
              ? "Np. balkon, blisko szkoły, bez remontu, parking…"
              : "Np. stan mieszkania, piętro, kiedy chcesz sprzedać…"
          }
          error={errors.message}
        />

        <Group title="Kontakt">
          <Input
            label="Imię"
            required
            autoComplete="given-name"
            value={contact.firstName}
            maxLength={80}
            onChange={(e) => setContact((c) => ({ ...c, firstName: e.target.value }))}
            error={errors.firstName}
          />
          <Input
            label="Nazwisko"
            required
            autoComplete="family-name"
            value={contact.lastName}
            maxLength={80}
            onChange={(e) => setContact((c) => ({ ...c, lastName: e.target.value }))}
            error={errors.lastName}
          />
          <PhoneInput
            label="Telefon"
            value={contact.phone}
            onChange={(phone) => setContact((c) => ({ ...c, phone }))}
            error={errors.phone}
            hint="Telefon lub e-mail — wystarczy jedno."
          />
          <Input
            label="E-mail"
            type="email"
            autoComplete="email"
            value={contact.email}
            maxLength={190}
            onChange={(e) => setContact((c) => ({ ...c, email: e.target.value }))}
            error={errors.email}
          />

          {/* Pułapka na boty: niewidoczna i poza kolejnością tabulacji. */}
          <div aria-hidden="true" className="absolute -left-[9999px] h-px w-px overflow-hidden">
            <label>
              Strona internetowa
              <input
                tabIndex={-1}
                autoComplete="off"
                value={website}
                onChange={(e) => setWebsite(e.target.value)}
              />
            </label>
          </div>
        </Group>

        <fieldset className="flex flex-col gap-3 border-t border-line pt-6">
          <legend className="sr-only">Zgody</legend>
          <Consent
            checked={consentProcessing}
            onChange={setConsentProcessing}
            required
            text={form.consentProcessingText}
            error={errors.consentProcessing}
          />
          <Consent
            checked={consentMarketing}
            onChange={setConsentMarketing}
            text={form.consentMarketingText}
          />
          <p className="text-[12px] leading-relaxed text-ink-muted">{form.privacyNote}</p>
        </fieldset>
      </div>

      <footer className="flex flex-col-reverse items-stretch gap-3 border-t border-line bg-subtle/50 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6">
        <p className="text-[12px] text-ink-muted">
          Pola z <span className="text-critical">*</span> są wymagane.
        </p>
        <Button type="submit" disabled={sending}>
          {sending ? "Wysyłanie…" : "Wyślij zgłoszenie"}
        </Button>
      </footer>
    </form>
  );
}

function Group({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="relative">
      <h2 className="mb-3 text-[13px] font-semibold tracking-tight text-ink">{title}</h2>
      <div className="grid gap-4 sm:grid-cols-2">{children}</div>
    </section>
  );
}

function Consent({
  checked,
  onChange,
  text,
  required,
  error,
}: {
  checked: boolean;
  onChange: (value: boolean) => void;
  text: string;
  required?: boolean;
  error?: string;
}) {
  return (
    <div className="flex flex-col gap-1">
      <label className="flex items-start gap-2.5 text-[13px] leading-relaxed text-ink">
        <input
          type="checkbox"
          checked={checked}
          onChange={(e) => onChange(e.target.checked)}
          aria-invalid={error ? true : undefined}
          className="mt-0.5 size-4 shrink-0 rounded border-line accent-accent"
        />
        <span>
          {text}
          {required ? (
            <span className="ml-0.5 text-critical">*</span>
          ) : (
            <span className="ml-1 text-ink-muted">(opcjonalnie)</span>
          )}
        </span>
      </label>
      {error && <p className="pl-6.5 text-[12px] text-critical">{error}</p>}
    </div>
  );
}
