import { useEffect, useState, type FormEvent } from "react";
import { X } from "lucide-react";
import {
  createEvent,
  updateEvent,
  type CalendarDictionaries,
  type CreateEventPayload,
  type EventDetail,
} from "../../api/calendar";
import { ApiError } from "../../api/client";
import type { ClientSummary } from "../../api/clients";
import { fetchDeal, type InterestEntry } from "../../api/deals";
import type { PropertySummary } from "../../api/properties";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { Textarea } from "../../components/ui/Textarea";
import { phoneError, phoneToField, phoneToPayload } from "../../lib/phone";
import { PhoneInput } from "../../components/ui/PhoneInput";
import { addDays, fromInputs, toDateInput, toTimeInput } from "./dates";

interface EventFormProps {
  dictionaries: CalendarDictionaries;
  /** Termin do edycji; pusty = nowy wpis. */
  initial: EventDetail | null;
  /** Dzień, w który kliknięto w siatce. Punkt startowy nowego terminu. */
  defaultDay: Date;
  properties: PropertySummary[];
  clients: ClientSummary[];
  /**
   * Podpowiedzi dla nowego terminu zakładanego spoza kalendarza. Z karty
   * transakcji przychodzą rodzaj, oferta, klient i sama transakcja.
   */
  preset?: EventPreset;
  /** Dostaje zapisany termin. Kalendarz otwiera go, gdy jest propozycja etapu. */
  onSaved: (saved: EventDetail) => void;
  onClose: () => void;
}

export interface EventPreset {
  type?: string;
  title?: string;
  propertyId?: string | null;
  clientId?: string | null;
  dealId?: string;
  dealTitle?: string;
}

/** Domyślna długość terminu: godzina. Tyle trwa prezentacja z dojazdem po niej. */
const DEFAULT_DURATION_MINUTES = 60;

export function EventForm({
  dictionaries,
  initial,
  defaultDay,
  properties,
  clients,
  preset,
  onSaved,
  onClose,
}: EventFormProps) {
  const start = initial ? new Date(initial.summary.startsAt) : defaultDay;
  const end = initial
    ? new Date(initial.summary.endsAt)
    : new Date(defaultDay.getTime() + DEFAULT_DURATION_MINUTES * 60_000);

  const [type, setType] = useState(
    initial?.summary.type ?? preset?.type ?? "PRESENTATION",
  );
  const [status, setStatus] = useState(initial?.summary.status ?? "PLANNED");
  const [title, setTitle] = useState(initial?.summary.title ?? preset?.title ?? "");
  const [allDay, setAllDay] = useState(initial?.summary.allDay ?? false);
  const [date, setDate] = useState(toDateInput(start));
  const [endDate, setEndDate] = useState(toDateInput(initial ? end : start));
  const [startTime, setStartTime] = useState(toTimeInput(start));
  const [endTime, setEndTime] = useState(toTimeInput(end));
  const [propertyId, setPropertyId] = useState(
    initial ? (initial.summary.propertyId ?? "") : (preset?.propertyId ?? ""),
  );
  const [clientId, setClientId] = useState(
    initial ? (initial.summary.clientId ?? "") : (preset?.clientId ?? ""),
  );
  // Transakcji nie wybiera się w formularzu. Przychodzi z karty Kanbana
  // i przy edycji zostaje taka, jaka była.
  const dealId = initial ? initial.summary.dealId : (preset?.dealId ?? null);
  const dealTitle = initial ? initial.summary.dealTitle : (preset?.dealTitle ?? null);

  // Uczestnicy to zainteresowani z tej transakcji. Wspólne oglądanie dwóch
  // osób to jeden termin, ale każda ma potem swój status na karcie.
  const [participantIds, setParticipantIds] = useState<string[]>(
    initial?.participants.map((participant) => participant.id) ?? [],
  );
  const [candidates, setCandidates] = useState<InterestEntry[]>([]);

  useEffect(() => {
    if (!dealId) return;
    const chosen = new Set(initial?.participants.map((participant) => participant.id));
    fetchDeal(dealId)
      .then((deal) =>
        // Ci, którzy odpadli, nie przyjdą na kolejne oglądanie. Chyba że już
        // są na tym terminie (edycja starszego wpisu).
        setCandidates(
          deal.interests.filter(
            (interest) => interest.status !== "DROPPED" || chosen.has(interest.id),
          ),
        ),
      )
      .catch(() => undefined);
  }, [dealId, initial]);

  function toggleParticipant(id: string) {
    setParticipantIds((current) =>
      current.includes(id) ? current.filter((existing) => existing !== id) : [...current, id],
    );
  }
  const [counterpartyName, setCounterpartyName] = useState(
    initial?.summary.counterpartyName ?? "",
  );
  const [counterpartyPhone, setCounterpartyPhone] = useState(
    phoneToField(initial?.summary.counterpartyPhone),
  );
  const [location, setLocation] = useState(
    // Przy powiązanej ofercie miejsce jest wyliczone z jej adresu. Nie wpisujemy
    // go z powrotem do pola, bo zapisałoby się jako kopia adresu.
    initial && !initial.summary.propertyId ? (initial.summary.location ?? "") : "",
  );
  const [description, setDescription] = useState(initial?.description ?? "");
  const [outcome, setOutcome] = useState(initial?.summary.outcome ?? "");
  const [outcomeNote, setOutcomeNote] = useState(initial?.outcomeNote ?? "");

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const happened = status === "COMPLETED";

  /** Te same reguły co w serwisie. Błąd ma być widoczny, zanim żądanie poleci. */
  function validate(startsAt: Date | null, endsAt: Date | null) {
    const found: Record<string, string> = {};

    if (!startsAt) found.date = "Podaj datę terminu.";
    if (startsAt && endsAt && endsAt <= startsAt) {
      found.endTime = "Koniec musi być późniejszy niż początek.";
    }
    const phoneProblem = phoneError(counterpartyPhone);
    if (phoneProblem) found.counterpartyPhone = phoneProblem;
    if (outcome && !happened) {
      found.outcome = "Rezultat można podać dopiero dla terminu, który się odbył.";
    }

    return found;
  }

  async function submit(formEvent: FormEvent) {
    formEvent.preventDefault();

    const startsAt = allDay
      ? fromInputs(date, "00:00")
      : fromInputs(date, startTime);
    const endsAt = allDay
      ? (() => {
          const last = fromInputs(endDate || date, "00:00");
          return last ? addDays(last, 1) : null;
        })()
      : fromInputs(date, endTime);

    const found = validate(startsAt, endsAt);
    if (Object.keys(found).length > 0 || !startsAt || !endsAt) {
      setErrors(found);
      return;
    }

    const payload: CreateEventPayload = {
      type,
      status,
      title: title.trim() || undefined,
      description: description.trim() || undefined,
      location: location.trim() || undefined,
      startsAt: startsAt.toISOString(),
      endsAt: endsAt.toISOString(),
      allDay,
      propertyId: propertyId || null,
      clientId: clientId || null,
      dealId,
      participantIds: dealId ? participantIds : [],
      counterpartyName: counterpartyName.trim() || undefined,
      counterpartyPhone: phoneToPayload(counterpartyPhone),
      outcome: happened && outcome ? outcome : null,
      outcomeNote: happened ? outcomeNote.trim() || undefined : undefined,
    };

    setSaving(true);
    setErrors({});
    setMessage(null);
    try {
      const saved = initial
        ? await updateEvent(initial.summary.id, payload)
        : await createEvent(payload);
      onSaved(saved);
    } catch (cause) {
      if (cause instanceof ApiError) {
        setErrors(cause.fieldErrors ?? {});
        setMessage(cause.message);
      } else {
        setMessage("Nie udało się zapisać terminu.");
      }
      setSaving(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-40 flex items-start justify-center overflow-y-auto bg-scrim/20 p-4 sm:p-8"
      role="dialog"
      aria-modal="true"
      aria-label={initial ? "Edycja terminu" : "Nowy termin"}
    >
      <form
        onSubmit={submit}
        className="card w-full max-w-2xl"
        onClick={(clickEvent) => clickEvent.stopPropagation()}
      >
        <header className="flex items-center justify-between border-b border-line px-4 py-3">
          <h2 className="text-[14px] font-semibold tracking-tight text-ink">
            {initial ? "Edytuj termin" : "Nowy termin"}
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

        <div className="grid grid-cols-1 gap-3 p-4 sm:grid-cols-2">
          {dealTitle && (
            <p className="rounded-md border border-line bg-subtle px-2.5 py-1.5 text-[12px] text-ink-secondary sm:col-span-2">
              Krok transakcji: <span className="font-medium text-ink">{dealTitle}</span>
            </p>
          )}

          <Select
            label="Rodzaj"
            required
            value={type}
            onChange={(changed) => setType(changed.target.value)}
            options={dictionaries.type}
            error={errors.type}
          />

          <Select
            label="Status"
            value={status}
            onChange={(changed) => setStatus(changed.target.value)}
            options={dictionaries.status}
            error={errors.status}
          />

          <Input
            label="Data"
            type="date"
            required
            value={date}
            onChange={(changed) => setDate(changed.target.value)}
            error={errors.date ?? errors.startsAt}
          />

          {allDay ? (
            <Input
              label="Do dnia (włącznie)"
              type="date"
              value={endDate}
              min={date}
              onChange={(changed) => setEndDate(changed.target.value)}
              error={errors.endsAt}
            />
          ) : (
            <div className="grid grid-cols-2 gap-3">
              <Input
                label="Od"
                type="time"
                required
                value={startTime}
                onChange={(changed) => setStartTime(changed.target.value)}
              />
              <Input
                label="Do"
                type="time"
                required
                value={endTime}
                onChange={(changed) => setEndTime(changed.target.value)}
                error={errors.endTime ?? errors.endsAt}
              />
            </div>
          )}

          <label className="flex items-center gap-2 text-[13px] text-ink sm:col-span-2">
            <input
              type="checkbox"
              checked={allDay}
              onChange={(changed) => setAllDay(changed.target.checked)}
              className="size-4 rounded border-line accent-accent"
            />
            Cały dzień
          </label>

          <div className="sm:col-span-2">
            <Input
              label="Tytuł"
              value={title}
              onChange={(changed) => setTitle(changed.target.value)}
              maxLength={120}
              placeholder="Zostaw puste. Złożymy z rodzaju i adresu oferty"
              error={errors.title}
            />
          </div>

          <Select
            label="Oferta"
            value={propertyId}
            onChange={(changed) => setPropertyId(changed.target.value)}
            placeholder="Bez powiązania"
            options={properties.map((property) => ({
              value: property.id,
              label: `${property.referenceNumber} · ${property.title}`,
            }))}
            hint="Adres i miejsce spotkania wezmą się z oferty."
            error={errors.propertyId}
          />

          <Select
            label="Klient"
            value={clientId}
            onChange={(changed) => setClientId(changed.target.value)}
            placeholder="Bez powiązania"
            options={clients.map((client) => ({
              value: client.id,
              label: `${client.firstName} ${client.lastName}`,
            }))}
            error={errors.clientId}
          />

          {candidates.length > 0 && (
            <fieldset className="flex flex-col gap-1.5 sm:col-span-2">
              <legend className="mb-1.5 text-[13px] font-medium text-ink">
                Uczestnicy z listy zainteresowanych
              </legend>
              <div className="flex flex-wrap gap-x-4 gap-y-1.5">
                {candidates.map((interest) => (
                  <label key={interest.id} className="flex items-center gap-2 text-[13px] text-ink">
                    <input
                      type="checkbox"
                      checked={participantIds.includes(interest.id)}
                      onChange={() => toggleParticipant(interest.id)}
                      className="size-4 rounded border-line accent-accent"
                    />
                    {interest.name}
                  </label>
                ))}
              </div>
              {errors.participantIds && (
                <p className="text-[12px] text-critical">{errors.participantIds}</p>
              )}
            </fieldset>
          )}

          {/*
            Strona popytu nie ma jeszcze swojej tabeli. Kupujący i najemcy
            wpisywani są tu z ręki. Patrz komentarz w encji CalendarEvent.
          */}
          <Input
            label="Druga strona"
            value={counterpartyName}
            onChange={(changed) => setCounterpartyName(changed.target.value)}
            placeholder="Kupujący, najemca, rzeczoznawca…"
            error={errors.counterpartyName}
          />

          <PhoneInput
            label="Telefon drugiej strony"
            value={counterpartyPhone}
            onChange={setCounterpartyPhone}
            error={errors.counterpartyPhone}
          />

          {!propertyId && (
            <div className="sm:col-span-2">
              <Input
                label="Miejsce"
                value={location}
                onChange={(changed) => setLocation(changed.target.value)}
                placeholder="Biuro, kancelaria, adres spotkania"
                error={errors.location}
              />
            </div>
          )}

          <div className="sm:col-span-2">
            <Textarea
              label="Opis"
              rows={3}
              value={description}
              onChange={(changed) => setDescription(changed.target.value)}
              placeholder="Ustalenia, na co zwrócić uwagę, kod do domofonu…"
              error={errors.description}
            />
          </div>

          {happened && (
            <>
              <Select
                label="Rezultat"
                value={outcome}
                onChange={(changed) => setOutcome(changed.target.value)}
                placeholder="Nie podano"
                options={dictionaries.outcome}
                error={errors.outcome}
              />
              <div className="sm:col-span-2">
                <Textarea
                  label="Notatka z przebiegu"
                  rows={2}
                  value={outcomeNote}
                  onChange={(changed) => setOutcomeNote(changed.target.value)}
                  error={errors.outcomeNote}
                />
              </div>
            </>
          )}
        </div>

        {message && (
          <p className="mx-4 mb-3 rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
            {message}
          </p>
        )}

        <footer className="flex justify-end gap-2 border-t border-line px-4 py-3">
          <Button type="button" variant="secondary" onClick={onClose} disabled={saving}>
            Anuluj
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? "Zapisywanie…" : initial ? "Zapisz zmiany" : "Dodaj termin"}
          </Button>
        </footer>
      </form>
    </div>
  );
}
