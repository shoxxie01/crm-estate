import { useState, type ChangeEvent, type FormEvent } from "react";
import { X } from "lucide-react";
import { ApiError } from "../../api/client";
import type { ClientSummary } from "../../api/clients";
import {
  CLOSED_STAGES,
  createDeal,
  updateDeal,
  type DealDetail,
  type DealDictionaries,
  type DealPayload,
} from "../../api/deals";
import type { PropertySummary } from "../../api/properties";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { Textarea } from "../../components/ui/Textarea";
import { formatAmount, maskAmount, parseAmount } from "../../lib/amount";

interface DealFormProps {
  dictionaries: DealDictionaries;
  /** Transakcja do edycji; pusta = nowa karta. */
  initial: DealDetail | null;
  /** Kolumna, w której kliknięto „+". Etap startowy nowej karty. */
  defaultStage: string;
  properties: PropertySummary[];
  clients: ClientSummary[];
  onSaved: (saved: DealDetail) => void;
  onClose: () => void;
}

const amountField = (value: number | null | undefined) =>
  value == null ? "" : formatAmount(String(Math.round(value)));

/**
 * Formularz karty. Nic nie jest wymagane. Karta ma powstać już przy pierwszym
 * telefonie. Tytuł, właściciela i wartość serwer podpowie z oferty.
 */
export function DealForm({
  dictionaries,
  initial,
  defaultStage,
  properties,
  clients,
  onSaved,
  onClose,
}: DealFormProps) {
  const card = initial?.card;

  const [title, setTitle] = useState(card?.title ?? "");
  const [stage, setStage] = useState(card?.stage ?? defaultStage);
  const [propertyId, setPropertyId] = useState(card?.propertyId ?? "");
  const [clientId, setClientId] = useState(card?.clientId ?? "");
  const [value, setValue] = useState(amountField(card?.value));
  const [commission, setCommission] = useState(amountField(card?.commission));
  const [notes, setNotes] = useState(initial?.notes ?? "");

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const openStages = dictionaries.stage.filter(
    (entry) => !CLOSED_STAGES.includes(entry.value),
  );
  const clientOptions = clients.map((client) => ({
    value: client.id,
    label: `${client.firstName} ${client.lastName}`,
  }));

  const selectedProperty = properties.find((property) => property.id === propertyId);

  const amountInput =
    (current: string, set: (next: string) => void) =>
    (event: ChangeEvent<HTMLInputElement>) => {
      const input = event.target;
      const next = maskAmount(
        input.value,
        input.selectionStart ?? input.value.length,
        current,
        (event.nativeEvent as InputEvent).inputType === "deleteContentForward",
      );
      input.value = next.value;
      input.setSelectionRange(next.caret, next.caret);
      set(next.value);
    };

  async function submit(formEvent: FormEvent) {
    formEvent.preventDefault();

    const payload: DealPayload = {
      title: title.trim() || undefined,
      stage: initial ? undefined : stage,
      propertyId: propertyId || null,
      clientId: clientId || null,
      value: parseAmount(value) ?? null,
      commission: parseAmount(commission) ?? null,
      notes: notes.trim() || undefined,
    };

    setSaving(true);
    setErrors({});
    setMessage(null);
    try {
      const saved = initial
        ? await updateDeal(initial.card.id, payload)
        : await createDeal(payload);
      onSaved(saved);
    } catch (cause) {
      if (cause instanceof ApiError) {
        setErrors(cause.fieldErrors ?? {});
        setMessage(cause.message);
      } else {
        setMessage("Nie udało się zapisać transakcji.");
      }
      setSaving(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-40 flex items-start justify-center overflow-y-auto bg-scrim/20 p-4 sm:p-8"
      role="dialog"
      aria-modal="true"
      aria-label={initial ? "Edycja transakcji" : "Nowa transakcja"}
    >
      <form onSubmit={submit} className="card w-full max-w-2xl">
        <header className="flex items-center justify-between border-b border-line px-4 py-3">
          <h2 className="text-[14px] font-semibold tracking-tight text-ink">
            {initial ? "Edytuj transakcję" : "Nowa transakcja"}
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
          <div className="sm:col-span-2">
            <Input
              label="Tytuł"
              value={title}
              onChange={(changed) => setTitle(changed.target.value)}
              maxLength={160}
              placeholder="Zostaw puste. Weźmiemy adres oferty albo nazwisko właściciela"
              error={errors.title}
            />
          </div>

          {/* Etap istniejącej karty zmienia się przeciągnięciem. Z historią. */}
          {!initial && (
            <Select
              label="Etap"
              value={stage}
              onChange={(changed) => setStage(changed.target.value)}
              options={openStages}
              error={errors.stage}
            />
          )}

          <Select
            label="Oferta"
            value={propertyId}
            onChange={(changed) => setPropertyId(changed.target.value)}
            placeholder="Jeszcze bez oferty"
            options={properties.map((property) => ({
              value: property.id,
              label: `${property.referenceNumber} · ${property.title}`,
            }))}
            hint={initial ? undefined : "Właściciel i cena podpowiedzą się z oferty."}
            error={errors.propertyId}
          />

          <Select
            label="Właściciel"
            value={clientId}
            onChange={(changed) => setClientId(changed.target.value)}
            placeholder={
              !initial && propertyId ? "Weź z oferty" : "Bez powiązania"
            }
            options={clientOptions}
            error={errors.clientId}
          />

          <Input
            label="Wartość transakcji"
            inputMode="numeric"
            value={value}
            onChange={amountInput(value, setValue)}
            trailing="zł"
            placeholder={
              !initial && selectedProperty?.price != null
                ? formatAmount(String(Math.round(selectedProperty.price)))
                : undefined
            }
            hint={!initial && propertyId ? "Puste = cena z oferty." : undefined}
            error={errors.value}
          />

          <Input
            label="Prowizja biura"
            inputMode="numeric"
            value={commission}
            onChange={amountInput(commission, setCommission)}
            trailing="zł"
            error={errors.commission}
          />

          <div className="sm:col-span-2">
            <Textarea
              label="Notatki"
              rows={3}
              value={notes}
              onChange={(changed) => setNotes(changed.target.value)}
              placeholder="Skąd lead, czego oczekuje właściciel, ustalenia…"
              error={errors.notes}
            />
          </div>
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
            {saving ? "Zapisywanie…" : initial ? "Zapisz zmiany" : "Dodaj transakcję"}
          </Button>
        </footer>
      </form>
    </div>
  );
}
