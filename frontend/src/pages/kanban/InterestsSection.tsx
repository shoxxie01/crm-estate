import { useState, type ChangeEvent, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { Pencil, Phone, Plus, Trash2 } from "lucide-react";
import { ApiError } from "../../api/client";
import type { ClientSummary } from "../../api/clients";
import {
  addInterest,
  deleteInterest,
  updateInterest,
  type DealDetail,
  type InterestEntry,
  type InterestPayload,
} from "../../api/deals";
import type { DictionaryEntry } from "../../api/properties";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { PhoneInput } from "../../components/ui/PhoneInput";
import { Select } from "../../components/ui/Select";
import { formatAmount, maskAmount, parseAmount } from "../../lib/amount";
import { formatCurrency } from "../../lib/format";
import { phoneError, phoneToField, phoneToPayload } from "../../lib/phone";

interface InterestsSectionProps {
  deal: DealDetail;
  statuses: DictionaryEntry[];
  clients: ClientSummary[];
  label: (value: string) => string;
  onChanged: (deal: DealDetail) => void;
}

/** Statusy, przy których pytamy o kwotę. Bez niej „złożył ofertę" niewiele mówi. */
const WITH_OFFER = ["OFFER", "ACCEPTED"];

const tone: Record<string, "neutral" | "accent" | "good" | "warning" | "critical"> = {
  NEW: "neutral",
  VIEWED: "neutral",
  CONSIDERING: "warning",
  OFFER: "accent",
  ACCEPTED: "good",
  DROPPED: "neutral",
};

/**
 * Zainteresowani ofertą. Mini-lejek popytu wewnątrz karty.
 *
 * Jedna oferta potrafi mieć kilku chętnych naraz: dwie osoby na oglądaniu,
 * jedna z ofertą, druga do namysłu. Każda ma tu własny status i kwotę.
 * Przyjęcie oferty ustawia kupującego karty i wartość transakcji.
 */
export function InterestsSection({
  deal,
  statuses,
  clients,
  label,
  onChanged,
}: InterestsSectionProps) {
  /** `null` = formularz zamknięty, `"new"` = dodawanie, inaczej edytowany wpis. */
  const [editing, setEditing] = useState<InterestEntry | "new" | null>(null);
  const [presetStatus, setPresetStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const dealId = deal.card.id;
  // Odpadli lądują na końcu. Wciąż widać, kto był, ale nie zasłaniają aktywnych.
  const sorted = [...deal.interests].sort(
    (a, b) => Number(a.status === "DROPPED") - Number(b.status === "DROPPED"),
  );

  async function quickStatus(interest: InterestEntry, status: string) {
    // Oferta bez kwoty to pół informacji. Otwieramy formularz z nowym statusem.
    if (WITH_OFFER.includes(status) && interest.offerAmount == null) {
      setPresetStatus(status);
      setEditing(interest);
      return;
    }
    setBusyId(interest.id);
    setError(null);
    try {
      onChanged(await updateInterest(dealId, interest.id, { ...toPayload(interest), status }));
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Nie udało się zmienić statusu.");
    } finally {
      setBusyId(null);
    }
  }

  async function remove(interest: InterestEntry) {
    setBusyId(interest.id);
    setError(null);
    try {
      onChanged(await deleteInterest(dealId, interest.id));
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Nie udało się usunąć.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <section className="flex flex-col gap-2">
      <div className="flex items-center justify-between">
        <h3 className="text-[12px] font-semibold tracking-tight text-ink">
          Zainteresowani{" "}
          <span className="font-normal text-ink-muted tabular-nums">{deal.interests.length}</span>
        </h3>
        {editing === null && (
          <Button
            size="sm"
            variant="ghost"
            onClick={() => {
              setPresetStatus(null);
              setEditing("new");
            }}
          >
            <Plus className="size-3.5" strokeWidth={2} />
            Dodaj
          </Button>
        )}
      </div>

      {deal.interests.length === 0 && editing === null && (
        <p className="text-[12px] text-ink-muted">
          Nikt się jeszcze nie zgłosił. Dodaj każdego, kto pyta o ofertę albo ją ogląda.
          Wtedy przy wspólnym oglądaniu każdy dostanie własny status.
        </p>
      )}

      {sorted.length > 0 && (
        <ul className="flex flex-col divide-y divide-line rounded-md border border-line">
          {sorted.map((interest) =>
            editing !== "new" && editing?.id === interest.id ? (
              <li key={interest.id} className="p-2.5">
                <InterestForm
                  dealId={dealId}
                  initial={interest}
                  presetStatus={presetStatus}
                  statuses={statuses}
                  clients={clients}
                  onSaved={(saved) => {
                    setEditing(null);
                    onChanged(saved);
                  }}
                  onCancel={() => setEditing(null)}
                />
              </li>
            ) : (
              <li
                key={interest.id}
                className={
                  interest.status === "DROPPED"
                    ? "flex flex-col gap-1 px-2.5 py-2 opacity-60"
                    : "flex flex-col gap-1 px-2.5 py-2"
                }
              >
                <div className="flex items-center gap-2">
                  {interest.clientId ? (
                    <Link
                      to={`/klienci/${interest.clientId}`}
                      className="truncate text-[13px] font-medium text-ink hover:underline"
                    >
                      {interest.name}
                    </Link>
                  ) : (
                    <span className="truncate text-[13px] font-medium text-ink">{interest.name}</span>
                  )}
                  <span className="ml-auto shrink-0">
                    <Badge tone={tone[interest.status] ?? "neutral"}>{label(interest.status)}</Badge>
                  </span>
                </div>

                <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[12px] text-ink-secondary">
                  {interest.phone && (
                    <a
                      href={`tel:${interest.phone.replace(/\s/g, "")}`}
                      className="flex items-center gap-1 text-accent hover:underline tabular-nums"
                    >
                      <Phone className="size-3" strokeWidth={2} />
                      {interest.phone}
                    </a>
                  )}
                  {interest.offerAmount != null && (
                    <span className="font-medium text-ink tabular-nums">
                      {formatCurrency(interest.offerAmount)}
                    </span>
                  )}
                </div>

                {interest.note && (
                  <p className="whitespace-pre-line text-[12px] text-ink-secondary">{interest.note}</p>
                )}

                <div className="flex items-center gap-1 pt-0.5">
                  <select
                    aria-label={`Status: ${interest.name}`}
                    value={interest.status}
                    disabled={busyId === interest.id}
                    onChange={(changed) => void quickStatus(interest, changed.target.value)}
                    className="h-7 rounded-md border border-line bg-surface px-1.5 text-[12px] text-ink"
                  >
                    {statuses.map((entry) => (
                      <option key={entry.value} value={entry.value}>
                        {entry.label}
                      </option>
                    ))}
                  </select>
                  <button
                    type="button"
                    onClick={() => {
                      setPresetStatus(null);
                      setEditing(interest);
                    }}
                    aria-label={`Edytuj: ${interest.name}`}
                    className="ml-auto rounded p-1 text-ink-muted hover:bg-subtle hover:text-ink"
                  >
                    <Pencil className="size-3.5" strokeWidth={2} />
                  </button>
                  <button
                    type="button"
                    onClick={() => void remove(interest)}
                    disabled={busyId === interest.id}
                    aria-label={`Usuń: ${interest.name}`}
                    className="rounded p-1 text-ink-muted hover:bg-critical/8 hover:text-critical"
                  >
                    <Trash2 className="size-3.5" strokeWidth={2} />
                  </button>
                </div>
              </li>
            ),
          )}
        </ul>
      )}

      {editing === "new" && (
        <div className="rounded-md border border-line p-2.5">
          <InterestForm
            dealId={dealId}
            initial={null}
            presetStatus={null}
            statuses={statuses}
            clients={clients.filter((client) => client.id !== deal.card.clientId)}
            onSaved={(saved) => {
              setEditing(null);
              onChanged(saved);
            }}
            onCancel={() => setEditing(null)}
          />
        </div>
      )}

      {error && (
        <p className="rounded-md border border-critical/30 bg-critical/8 px-2.5 py-1.5 text-[12px] text-critical">
          {error}
        </p>
      )}
    </section>
  );
}

function toPayload(interest: InterestEntry): InterestPayload {
  return {
    clientId: interest.clientId,
    name: interest.clientId ? undefined : interest.name,
    phone: interest.clientId ? undefined : (interest.phone ?? undefined),
    status: interest.status,
    offerAmount: interest.offerAmount,
    note: interest.note ?? undefined,
  };
}

interface InterestFormProps {
  dealId: string;
  initial: InterestEntry | null;
  /** Status wybrany z listy, który wymaga kwoty. Formularz startuje od niego. */
  presetStatus: string | null;
  statuses: DictionaryEntry[];
  clients: ClientSummary[];
  onSaved: (deal: DealDetail) => void;
  onCancel: () => void;
}

function InterestForm({
  dealId,
  initial,
  presetStatus,
  statuses,
  clients,
  onSaved,
  onCancel,
}: InterestFormProps) {
  const [fromBase, setFromBase] = useState(initial?.clientId != null);
  const [clientId, setClientId] = useState(initial?.clientId ?? "");
  const [name, setName] = useState(initial && !initial.clientId ? initial.name : "");
  const [phone, setPhone] = useState(
    phoneToField(initial && !initial.clientId ? initial.phone : null),
  );
  const [status, setStatus] = useState(presetStatus ?? initial?.status ?? "NEW");
  const [offer, setOffer] = useState(
    initial?.offerAmount != null ? formatAmount(String(Math.round(initial.offerAmount))) : "",
  );
  const [note, setNote] = useState(initial?.note ?? "");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  function onOffer(event: ChangeEvent<HTMLInputElement>) {
    const input = event.target;
    const next = maskAmount(
      input.value,
      input.selectionStart ?? input.value.length,
      offer,
      (event.nativeEvent as InputEvent).inputType === "deleteContentForward",
    );
    input.value = next.value;
    input.setSelectionRange(next.caret, next.caret);
    setOffer(next.value);
  }

  async function submit(formEvent: FormEvent) {
    formEvent.preventDefault();

    const found: Record<string, string> = {};
    if (fromBase && !clientId) found.clientId = "Wybierz klienta.";
    if (!fromBase && !name.trim()) found.name = "Wpisz imię i nazwisko.";
    const phoneProblem = fromBase ? null : phoneError(phone);
    if (phoneProblem) found.phone = phoneProblem;
    if (Object.keys(found).length > 0) {
      setErrors(found);
      return;
    }

    const payload: InterestPayload = {
      clientId: fromBase ? clientId : null,
      name: fromBase ? undefined : name.trim(),
      phone: fromBase ? undefined : phoneToPayload(phone),
      status,
      offerAmount: parseAmount(offer) ?? null,
      note: note.trim() || undefined,
    };

    setSaving(true);
    setErrors({});
    setMessage(null);
    try {
      onSaved(
        initial
          ? await updateInterest(dealId, initial.id, payload)
          : await addInterest(dealId, payload),
      );
    } catch (cause) {
      if (cause instanceof ApiError) {
        setErrors(cause.fieldErrors ?? {});
        setMessage(cause.message);
      } else {
        setMessage("Nie udało się zapisać.");
      }
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-2.5">
      <div className="flex rounded-md border border-line bg-surface p-0.5" role="group">
        {[
          { value: false, text: "Spoza bazy" },
          { value: true, text: "Klient z bazy" },
        ].map((option) => (
          <button
            key={option.text}
            type="button"
            onClick={() => setFromBase(option.value)}
            aria-pressed={fromBase === option.value}
            className={
              fromBase === option.value
                ? "flex-1 rounded bg-subtle px-2 py-1 text-[12px] font-medium text-ink"
                : "flex-1 rounded px-2 py-1 text-[12px] text-ink-secondary hover:text-ink"
            }
          >
            {option.text}
          </button>
        ))}
      </div>

      {fromBase ? (
        <Select
          label="Klient"
          value={clientId}
          onChange={(changed) => setClientId(changed.target.value)}
          placeholder="Wybierz"
          options={clients.map((client) => ({
            value: client.id,
            label: `${client.firstName} ${client.lastName}`,
          }))}
          error={errors.clientId}
        />
      ) : (
        <>
          <Input
            label="Imię i nazwisko"
            value={name}
            onChange={(changed) => setName(changed.target.value)}
            maxLength={160}
            placeholder="Np. Ewa z ogłoszenia na Otodom"
            error={errors.name}
          />
          <PhoneInput label="Telefon" value={phone} onChange={setPhone} error={errors.phone} />
        </>
      )}

      <div className="grid grid-cols-2 gap-2.5">
        <Select
          label="Status"
          value={status}
          onChange={(changed) => setStatus(changed.target.value)}
          options={statuses}
          error={errors.status}
        />
        <Input
          label="Oferta"
          inputMode="numeric"
          value={offer}
          onChange={onOffer}
          trailing="zł"
          hint={status === "ACCEPTED" ? "Stanie się wartością transakcji." : undefined}
          error={errors.offerAmount}
        />
      </div>

      <Input
        label="Notatka"
        value={note}
        onChange={(changed) => setNote(changed.target.value)}
        maxLength={2000}
        placeholder="Kredyt czy gotówka, termin wyprowadzki…"
        error={errors.note}
      />

      {message && (
        <p className="rounded-md border border-critical/30 bg-critical/8 px-2.5 py-1.5 text-[12px] text-critical">
          {message}
        </p>
      )}

      <div className="flex justify-end gap-2">
        <Button type="button" size="sm" variant="ghost" onClick={onCancel} disabled={saving}>
          Anuluj
        </Button>
        <Button type="submit" size="sm" disabled={saving}>
          {saving ? "Zapisywanie…" : initial ? "Zapisz" : "Dodaj"}
        </Button>
      </div>
    </form>
  );
}
