import { useState, type FormEvent } from "react";
import { CalendarPlus, Check, FileText, Plus, RotateCcw, Trash2 } from "lucide-react";
import { ApiError } from "../../api/client";
import {
  addDeadline,
  deleteDeadline,
  markDeadlineMet,
  moveDeadline,
  reopenDeadline,
  type DealDetail,
  type DeadlineEntry,
} from "../../api/deals";
import type { DictionaryEntry } from "../../api/properties";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { cn } from "../../lib/cn";
import {
  DEADLINE_WARN_DAYS,
  RENEWABLE_DEADLINES,
  daysUntil,
  deadlineTone,
  deadlineWhen,
  formatDueDate,
  suggestedDeadlineType,
} from "./dealMeta";

interface DeadlinesSectionProps {
  deal: DealDetail;
  types: DictionaryEntry[];
  label: (value: string) => string;
  onChanged: (deal: DealDetail) => void;
  /** Zaplanowanie rozmowy o przedłużeniu umowy. Formularz terminu z tytułem. */
  onScheduleRenewal: (title: string) => void;
}

/**
 * Terminy umowne karty. Daty z umów, których biuro pilnuje. Otwarte na górze
 * od najbliższego; dotrzymane i przesunięte niżej, jako historia (aneksy).
 */
export function DeadlinesSection({
  deal,
  types,
  label,
  onChanged,
  onScheduleRenewal,
}: DeadlinesSectionProps) {
  const [adding, setAdding] = useState(false);
  const [movingId, setMovingId] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const dealId = deal.card.id;
  const closed = deal.card.closedAt != null;
  const open = deal.deadlines.filter((entry) => entry.status === "OPEN");
  const past = deal.deadlines.filter((entry) => entry.status !== "OPEN");

  async function run(id: string, action: () => Promise<DealDetail>) {
    setBusyId(id);
    setError(null);
    try {
      onChanged(await action());
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Nie udało się zapisać zmiany.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <section className="flex flex-col gap-2">
      <div className="flex items-center justify-between">
        <h3 className="text-[12px] font-semibold tracking-tight text-ink">Terminy umowne</h3>
        {!adding && !closed && (
          <Button size="sm" variant="ghost" onClick={() => setAdding(true)}>
            <Plus className="size-3.5" strokeWidth={2} />
            Dodaj
          </Button>
        )}
      </div>

      {deal.deadlines.length === 0 && !adding && (
        <p className="text-[12px] text-ink-muted">
          Wpisz datę końca umowy pośrednictwa, ważności rezerwacji albo aktu. Tablica
          ostrzeże {DEADLINE_WARN_DAYS} dni wcześniej, a data pojawi się w kalendarzu.
        </p>
      )}

      {open.length > 0 && (
        <ul className="flex flex-col divide-y divide-line rounded-md border border-line">
          {open.map((entry) => {
            const days = daysUntil(entry.dueDate);
            const tone = deadlineTone(days);
            const renewable =
              RENEWABLE_DEADLINES.includes(entry.type) && days <= DEADLINE_WARN_DAYS && !closed;
            return (
              <li key={entry.id} className="flex flex-col gap-1.5 px-2.5 py-2">
                <div className="flex items-start gap-2">
                  <FileText className="mt-0.5 size-3.5 shrink-0 text-ink-muted" strokeWidth={2} />
                  <div className="min-w-0 flex-1">
                    <p className="text-[13px] text-ink">{label(entry.type)}</p>
                    <p className="text-[12px] text-ink-secondary tabular-nums">
                      {formatDueDate(entry.dueDate)}
                      {entry.movedFromDate && (
                        <span className="text-ink-muted"> · przesunięty z {formatDueDate(entry.movedFromDate)}</span>
                      )}
                    </p>
                    {entry.note && <p className="text-[12px] text-ink-muted">{entry.note}</p>}
                  </div>
                  <Badge tone={tone}>{deadlineWhen(days)}</Badge>
                </div>

                {movingId === entry.id ? (
                  <MoveForm
                    current={entry}
                    busy={busyId === entry.id}
                    onCancel={() => setMovingId(null)}
                    onSubmit={(dueDate, note) =>
                      run(entry.id, async () => {
                        const saved = await moveDeadline(dealId, entry.id, { dueDate, note });
                        setMovingId(null);
                        return saved;
                      })
                    }
                  />
                ) : (
                  <div className="flex flex-wrap items-center gap-1">
                    <Button
                      size="sm"
                      variant="secondary"
                      disabled={busyId === entry.id}
                      onClick={() => run(entry.id, () => markDeadlineMet(dealId, entry.id))}
                    >
                      <Check className="size-3.5" strokeWidth={2} />
                      Dotrzymany
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      disabled={busyId === entry.id}
                      onClick={() => setMovingId(entry.id)}
                    >
                      Przesuń
                    </Button>
                    {renewable && (
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => onScheduleRenewal(`Przedłużenie: ${label(entry.type).toLowerCase()}`)}
                      >
                        <CalendarPlus className="size-3.5" strokeWidth={2} />
                        Umów rozmowę o przedłużeniu
                      </Button>
                    )}
                    <button
                      type="button"
                      onClick={() => run(entry.id, () => deleteDeadline(dealId, entry.id))}
                      disabled={busyId === entry.id}
                      aria-label={`Usuń: ${label(entry.type)}`}
                      className="ml-auto rounded p-1 text-ink-muted hover:bg-critical/8 hover:text-critical"
                    >
                      <Trash2 className="size-3.5" strokeWidth={2} />
                    </button>
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}

      {adding && (
        <AddForm
          deal={deal}
          types={types}
          onCancel={() => setAdding(false)}
          onSaved={(saved) => {
            setAdding(false);
            onChanged(saved);
          }}
        />
      )}

      {past.length > 0 && (
        <ul className="flex flex-col gap-1">
          {past.map((entry) => (
            <li
              key={entry.id}
              className={cn(
                "flex items-center gap-2 text-[12px]",
                entry.status === "MOVED" ? "text-ink-muted line-through" : "text-ink-secondary",
              )}
            >
              <span className="truncate">
                {label(entry.type)} · {formatDueDate(entry.dueDate)}
              </span>
              <span className="ml-auto shrink-0 no-underline">
                {entry.status === "MET" ? "dotrzymany" : "przesunięty"}
              </span>
              {entry.status === "MET" && (
                <button
                  type="button"
                  onClick={() => run(entry.id, () => reopenDeadline(dealId, entry.id))}
                  disabled={busyId === entry.id}
                  aria-label="Cofnij „dotrzymany”"
                  title="Cofnij „dotrzymany”"
                  className="rounded p-0.5 text-ink-muted hover:bg-subtle hover:text-ink"
                >
                  <RotateCcw className="size-3" strokeWidth={2} />
                </button>
              )}
            </li>
          ))}
        </ul>
      )}

      {error && (
        <p className="rounded-md border border-critical/30 bg-critical/8 px-2.5 py-1.5 text-[12px] text-critical">
          {error}
        </p>
      )}
    </section>
  );
}

function AddForm({
  deal,
  types,
  onCancel,
  onSaved,
}: {
  deal: DealDetail;
  types: DictionaryEntry[];
  onCancel: () => void;
  onSaved: (deal: DealDetail) => void;
}) {
  const [type, setType] = useState(suggestedDeadlineType(deal.card.stage));
  const [dueDate, setDueDate] = useState("");
  const [note, setNote] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!dueDate) {
      setErrors({ dueDate: "Podaj datę terminu." });
      return;
    }
    setSaving(true);
    setErrors({});
    setMessage(null);
    try {
      onSaved(await addDeadline(deal.card.id, { type, dueDate, note: note.trim() || undefined }));
    } catch (cause) {
      if (cause instanceof ApiError) {
        setErrors(cause.fieldErrors ?? {});
        setMessage(cause.message);
      } else {
        setMessage("Nie udało się dodać terminu.");
      }
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-2.5 rounded-md border border-line p-2.5">
      <Select
        label="Rodzaj"
        value={type}
        onChange={(changed) => setType(changed.target.value)}
        options={types}
        error={errors.type}
      />
      <Input
        label="Data"
        type="date"
        value={dueDate}
        onChange={(changed) => setDueDate(changed.target.value)}
        error={errors.dueDate}
      />
      <Input
        label="Notatka"
        value={note}
        onChange={(changed) => setNote(changed.target.value)}
        maxLength={2000}
        placeholder="Np. wyłączność, numer umowy, kancelaria"
      />
      {message && <p className="text-[12px] text-critical">{message}</p>}
      <div className="flex justify-end gap-2">
        <Button type="button" size="sm" variant="ghost" onClick={onCancel} disabled={saving}>
          Anuluj
        </Button>
        <Button type="submit" size="sm" disabled={saving}>
          {saving ? "Zapisywanie…" : "Dodaj termin"}
        </Button>
      </div>
    </form>
  );
}

/** Przesunięcie terminu (aneks). Stara data zostaje w historii karty. */
function MoveForm({
  current,
  busy,
  onCancel,
  onSubmit,
}: {
  current: DeadlineEntry;
  busy: boolean;
  onCancel: () => void;
  onSubmit: (dueDate: string, note: string | undefined) => void;
}) {
  const [dueDate, setDueDate] = useState(current.dueDate);
  const [note, setNote] = useState("");

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit(dueDate, note.trim() || undefined);
      }}
      className="flex flex-col gap-2"
    >
      <div className="grid grid-cols-2 gap-2">
        <Input label="Nowa data" type="date" value={dueDate} onChange={(c) => setDueDate(c.target.value)} />
        <Input label="Powód" value={note} onChange={(c) => setNote(c.target.value)} placeholder="Np. aneks nr 1" />
      </div>
      <div className="flex justify-end gap-2">
        <Button type="button" size="sm" variant="ghost" onClick={onCancel} disabled={busy}>
          Anuluj
        </Button>
        <Button type="submit" size="sm" disabled={busy || !dueDate || dueDate === current.dueDate}>
          Przesuń termin
        </Button>
      </div>
    </form>
  );
}
