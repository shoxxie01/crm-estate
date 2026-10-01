import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  AlertTriangle,
  ArrowRight,
  Building2,
  CalendarClock,
  Kanban,
  Pencil,
  Phone,
  Trash2,
  User,
  X,
} from "lucide-react";
import {
  deleteEvent,
  fetchEvent,
  updateEventStatus,
  type CalendarDictionaries,
  type EventDetail,
} from "../../api/calendar";
import { ApiError } from "../../api/client";
import { changeDealStage } from "../../api/deals";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { cn } from "../../lib/cn";
import { formatDayLong, timeRange } from "./dates";
import { badgeTone, eventIcon, statusTone, typeInk } from "./eventMeta";

interface EventPanelProps {
  eventId: string;
  dictionaries: CalendarDictionaries | null;
  label: (value: string) => string;
  onEdit: (event: EventDetail) => void;
  onChanged: () => void;
  onClose: () => void;
}

/**
 * Panel szczegółów terminu.
 *
 * Poza podglądem robi jedną rzecz, o którą chodzi w kalendarzu CRM-u najbardziej:
 * pozwala domknąć termin dwoma kliknięciami. Rezultat wpisany zaraz po wizycie
 * jest wart tyle, ile cała reszta modułu. Wpisany po tygodniu nie jest wart nic.
 */
export function EventPanel({
  eventId,
  dictionaries,
  label,
  onEdit,
  onChanged,
  onClose,
}: EventPanelProps) {
  const [event, setEvent] = useState<EventDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [closing, setClosing] = useState(false);
  const [outcome, setOutcome] = useState("");
  const [outcomeNote, setOutcomeNote] = useState("");
  const [confirmDelete, setConfirmDelete] = useState(false);
  /** Odrzucone propozycje. W obrębie panelu, żeby „Nie teraz" nie wracało przy każdej zmianie. */
  const [dismissed, setDismissed] = useState<string | null>(null);
  const [moving, setMoving] = useState(false);

  useEffect(() => {
    setLoading(true);
    setClosing(false);
    setConfirmDelete(false);
    fetchEvent(eventId)
      .then((result) => {
        setEvent(result);
        setOutcome(result.summary.outcome ?? "");
        setOutcomeNote(result.outcomeNote ?? "");
        setError(null);
      })
      .catch((cause) =>
        setError(
          cause instanceof ApiError ? cause.message : "Nie udało się wczytać terminu.",
        ),
      )
      .finally(() => setLoading(false));
  }, [eventId]);

  async function changeStatus(status: string, withOutcome = false) {
    setBusy(true);
    setError(null);
    try {
      const updated = await updateEventStatus(eventId, {
        status,
        outcome: withOutcome && outcome ? outcome : null,
        outcomeNote: withOutcome ? outcomeNote : undefined,
      });
      setEvent(updated);
      setClosing(false);
      onChanged();
    } catch (cause) {
      setError(
        cause instanceof ApiError ? cause.message : "Nie udało się zmienić statusu.",
      );
    } finally {
      setBusy(false);
    }
  }

  /** Przeniesienie transakcji zgodnie z propozycją. Jedno kliknięcie z kalendarza. */
  async function acceptSuggestion() {
    const suggestion = event?.suggestion;
    if (!suggestion) return;
    setMoving(true);
    setError(null);
    try {
      await changeDealStage(suggestion.dealId, { stage: suggestion.toStage });
      // Odczyt od nowa: propozycja zniknie sama, bo karta jest już w tym etapie.
      setEvent(await fetchEvent(eventId));
      onChanged();
    } catch (cause) {
      setError(
        cause instanceof ApiError ? cause.message : "Nie udało się przenieść transakcji.",
      );
    } finally {
      setMoving(false);
    }
  }

  async function remove() {
    setBusy(true);
    try {
      await deleteEvent(eventId);
      onChanged();
      onClose();
    } catch (cause) {
      setError(
        cause instanceof ApiError ? cause.message : "Nie udało się usunąć terminu.",
      );
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <aside className="card p-4 text-[13px] text-ink-muted">Wczytywanie…</aside>
    );
  }

  if (!event) {
    return (
      <aside className="card p-4">
        <p className="text-[13px] text-critical">{error ?? "Nie znaleziono terminu."}</p>
      </aside>
    );
  }

  const summary = event.summary;
  const Icon = eventIcon(summary.type);
  const tone = statusTone(summary.status);
  const starts = new Date(summary.startsAt);

  return (
    <aside className="card flex flex-col">
      <header className="flex items-start justify-between gap-2 border-b border-line px-4 py-3">
        <div className="min-w-0">
          <div className="flex items-center gap-1.5">
            <Icon className={cn("size-4 shrink-0", typeInk(summary.type))} strokeWidth={2} />
            <span className={cn("text-[12px] font-medium", typeInk(summary.type))}>
              {label(summary.type)}
            </span>
            <Badge tone={badgeTone[tone]}>{label(summary.status)}</Badge>
          </div>
          <h2 className="mt-1 text-[14px] font-semibold tracking-tight text-ink">
            {summary.title}
          </h2>
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label="Zamknij szczegóły"
          className="rounded p-1 text-ink-muted hover:bg-subtle hover:text-ink"
        >
          <X className="size-4" strokeWidth={2} />
        </button>
      </header>

      <div className="flex flex-col gap-3 p-4">
        <div className="flex items-start gap-2 text-[13px] text-ink">
          <CalendarClock className="mt-0.5 size-4 shrink-0 text-ink-muted" strokeWidth={2} />
          <span className="first-letter:uppercase">
            {formatDayLong(starts)} ·{" "}
            <span className="tabular-nums">
              {timeRange(summary.startsAt, summary.endsAt, summary.allDay)}
            </span>
          </span>
        </div>

        {summary.location && (
          <p className="text-[13px] text-ink-secondary">{summary.location}</p>
        )}

        {/* Powiązania. Sedno modułu: z terminu ma być jedno kliknięcie do oferty i klienta. */}
        {summary.propertyId && (
          <Link
            to={`/nieruchomosci/${summary.propertyId}`}
            className="flex items-start gap-2 rounded-md border border-line px-2.5 py-2 text-[13px] transition-colors hover:border-line-strong hover:bg-subtle"
          >
            <Building2 className="mt-0.5 size-4 shrink-0 text-ink-muted" strokeWidth={2} />
            <span className="min-w-0">
              <span className="block truncate text-ink">{summary.propertyTitle}</span>
              <span className="block text-[12px] text-ink-muted tabular-nums">
                {summary.propertyReference}
              </span>
            </span>
          </Link>
        )}

        {summary.clientId && (
          <Link
            to={`/klienci/${summary.clientId}`}
            className="flex items-center gap-2 rounded-md border border-line px-2.5 py-2 text-[13px] transition-colors hover:border-line-strong hover:bg-subtle"
          >
            <User className="size-4 shrink-0 text-ink-muted" strokeWidth={2} />
            <span className="truncate text-ink">{summary.clientName}</span>
            <span className="ml-auto shrink-0 text-[11px] text-ink-muted">właściciel</span>
          </Link>
        )}

        {summary.dealId && (
          <Link
            to={`/kanban?karta=${summary.dealId}`}
            className="flex items-center gap-2 rounded-md border border-line px-2.5 py-2 text-[13px] transition-colors hover:border-line-strong hover:bg-subtle"
          >
            <Kanban className="size-4 shrink-0 text-ink-muted" strokeWidth={2} />
            <span className="truncate text-ink">{summary.dealTitle}</span>
            <span className="ml-auto shrink-0 text-[11px] text-ink-muted">transakcja</span>
          </Link>
        )}

        {event.participants.length > 0 && (
          <div className="rounded-md border border-line px-2.5 py-2">
            <p className="text-[12px] font-medium text-ink">
              Uczestnicy · {event.participants.length}
            </p>
            <ul className="mt-1 flex flex-col gap-0.5">
              {event.participants.map((participant) => (
                <li key={participant.id} className="flex items-center gap-2 text-[12px]">
                  <span className="truncate text-ink">{participant.name}</span>
                  <span className="ml-auto shrink-0 text-ink-muted">
                    {label(participant.status)}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {summary.counterpartyName && (
          <div className="flex items-center gap-2 rounded-md border border-line px-2.5 py-2 text-[13px]">
            <User className="size-4 shrink-0 text-ink-muted" strokeWidth={2} />
            <span className="truncate text-ink">{summary.counterpartyName}</span>
            {summary.counterpartyPhone && (
              <a
                href={`tel:${summary.counterpartyPhone.replace(/\s/g, "")}`}
                className="ml-auto flex shrink-0 items-center gap-1 text-[12px] text-accent hover:underline tabular-nums"
              >
                <Phone className="size-3" strokeWidth={2} />
                {summary.counterpartyPhone}
              </a>
            )}
          </div>
        )}

        {event.description && (
          <p className="whitespace-pre-line text-[13px] text-ink-secondary">
            {event.description}
          </p>
        )}

        {summary.outcome && (
          <div className="rounded-md border border-line bg-subtle px-2.5 py-2">
            <p className="text-[12px] font-medium text-ink">
              Rezultat: {label(summary.outcome)}
            </p>
            {event.outcomeNote && (
              <p className="mt-1 whitespace-pre-line text-[12px] text-ink-secondary">
                {event.outcomeNote}
              </p>
            )}
          </div>
        )}

        {event.suggestion &&
          dismissed !== `${event.suggestion.dealId}:${event.suggestion.toStage}` && (
            <div className="rounded-md border border-accent-ring/60 bg-accent-subtle px-2.5 py-2">
              <p className="text-[12px] text-ink-secondary">{event.suggestion.reason}</p>
              <p className="mt-1 flex flex-wrap items-center gap-1 text-[12px] font-medium text-ink">
                Przenieść „{event.suggestion.dealTitle}”:
                <span className="whitespace-nowrap">
                  {label(event.suggestion.fromStage)}{" "}
                  <ArrowRight className="inline size-3" strokeWidth={2} />{" "}
                  {label(event.suggestion.toStage)}
                </span>
              </p>
              <div className="mt-2 flex gap-2">
                <Button size="sm" onClick={acceptSuggestion} disabled={moving}>
                  {moving ? "Przenoszenie…" : "Przenieś kartę"}
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() =>
                    setDismissed(`${event.suggestion!.dealId}:${event.suggestion!.toStage}`)
                  }
                  disabled={moving}
                >
                  Nie teraz
                </Button>
              </div>
            </div>
          )}

        {/* Kolizje ostrzegają, nie blokują. Decyzja należy do agenta. */}
        {event.conflicts.length > 0 && (
          <div className="rounded-md border border-warning/40 bg-warning/12 px-2.5 py-2">
            <p className="flex items-center gap-1.5 text-[12px] font-medium text-warning-ink">
              <AlertTriangle className="size-3.5" strokeWidth={2} />
              Nakłada się na {event.conflicts.length}{" "}
              {event.conflicts.length === 1 ? "inny termin" : "inne terminy"}
            </p>
            <ul className="mt-1 flex flex-col gap-0.5">
              {event.conflicts.map((conflict) => (
                <li key={conflict.id} className="text-[12px] text-ink-secondary">
                  <span className="tabular-nums">
                    {timeRange(conflict.startsAt, conflict.endsAt, conflict.allDay)}
                  </span>{" "}
                  · {conflict.title}
                  {conflict.agentId !== summary.agentId && (
                    <span className="text-ink-muted"> · {conflict.agentName}</span>
                  )}
                </li>
              ))}
            </ul>
          </div>
        )}

        <p className="text-[11px] text-ink-muted">
          Agent: {summary.agentName} · dodał(a) {event.createdByName}
        </p>

        {error && (
          <p className="rounded-md border border-critical/30 bg-critical/8 px-2.5 py-1.5 text-[12px] text-critical">
            {error}
          </p>
        )}
      </div>

      <footer className="mt-auto flex flex-col gap-2 border-t border-line px-4 py-3">
        {closing ? (
          <div className="flex flex-col gap-2">
            <label className="text-[12px] font-medium text-ink" htmlFor="outcome">
              Rezultat
            </label>
            <select
              id="outcome"
              value={outcome}
              onChange={(changed) => setOutcome(changed.target.value)}
              className="h-8 w-full rounded-md border border-line bg-surface px-2 text-[13px] text-ink"
            >
              <option value="">Nie podano</option>
              {dictionaries?.outcome.map((entry) => (
                <option key={entry.value} value={entry.value}>
                  {entry.label}
                </option>
              ))}
            </select>
            <textarea
              value={outcomeNote}
              onChange={(changed) => setOutcomeNote(changed.target.value)}
              rows={2}
              placeholder="Notatka z przebiegu (opcjonalnie)"
              className="w-full rounded-md border border-line bg-surface px-2 py-1.5 text-[13px] text-ink placeholder:text-ink-muted"
            />
            <div className="flex gap-2">
              <Button size="sm" onClick={() => changeStatus("COMPLETED", true)} disabled={busy}>
                Zapisz rezultat
              </Button>
              <Button size="sm" variant="ghost" onClick={() => setClosing(false)} disabled={busy}>
                Anuluj
              </Button>
            </div>
          </div>
        ) : (
          <div className="flex flex-wrap gap-2">
            {summary.status !== "CONFIRMED" && summary.status !== "COMPLETED" && (
              <Button
                size="sm"
                variant="secondary"
                onClick={() => changeStatus("CONFIRMED")}
                disabled={busy}
              >
                Potwierdzony
              </Button>
            )}
            <Button size="sm" onClick={() => setClosing(true)} disabled={busy}>
              Odbył się
            </Button>
            {summary.status !== "NO_SHOW" && (
              <Button
                size="sm"
                variant="secondary"
                onClick={() => changeStatus("NO_SHOW")}
                disabled={busy}
              >
                Nie stawił się
              </Button>
            )}
            {summary.status !== "CANCELLED" && (
              <Button
                size="sm"
                variant="ghost"
                onClick={() => changeStatus("CANCELLED")}
                disabled={busy}
              >
                Odwołaj
              </Button>
            )}
          </div>
        )}

        <div className="flex items-center gap-2 border-t border-line pt-2">
          <Button size="sm" variant="secondary" onClick={() => onEdit(event)} disabled={busy}>
            <Pencil className="size-3.5" strokeWidth={2} />
            Edytuj
          </Button>

          {confirmDelete ? (
            <span className="flex items-center gap-2 text-[12px] text-ink-secondary">
              Usunąć?
              <Button
                size="sm"
                variant="secondary"
                onClick={remove}
                disabled={busy}
                className="border-critical/40 text-critical hover:bg-critical/8"
              >
                Tak
              </Button>
              <Button size="sm" variant="ghost" onClick={() => setConfirmDelete(false)}>
                Nie
              </Button>
            </span>
          ) : (
            <Button
              size="sm"
              variant="ghost"
              onClick={() => setConfirmDelete(true)}
              disabled={busy}
              className="ml-auto text-critical hover:bg-critical/8"
            >
              <Trash2 className="size-3.5" strokeWidth={2} />
              Usuń
            </Button>
          )}
        </div>
      </footer>
    </aside>
  );
}
