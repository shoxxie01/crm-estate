import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  AlertTriangle,
  Building2,
  CalendarPlus,
  Pencil,
  Trash2,
  User,
  X,
} from "lucide-react";
import { ApiError } from "../../api/client";
import type { ClientSummary } from "../../api/clients";
import { deleteDeal, fetchDeal, type DealDetail, type DealDictionaries } from "../../api/deals";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { ConfirmDialog } from "../../components/ui/ConfirmDialog";
import { formatCurrency } from "../../lib/format";
import { cn } from "../../lib/cn";
import { formatDayMonth, timeRange } from "../calendar/dates";
import { badgeTone, eventIcon, statusTone, typeInk } from "../calendar/eventMeta";
import { daysInStage, daysLabel, staleness, whenLabel } from "./dealMeta";
import { InterestsSection } from "./InterestsSection";
import { DeadlinesSection } from "./DeadlinesSection";

interface DealPanelProps {
  dealId: string;
  /** Zwiększany przez tablicę po każdej zmianie. Panel wtedy dociąga kartę od nowa. */
  version: number;
  dictionaries: DealDictionaries | null;
  clients: ClientSummary[];
  label: (value: string) => string;
  /** Zmiana w samym panelu (zainteresowani). Tablica odświeża liczniki. */
  onChanged: () => void;
  onEdit: (deal: DealDetail) => void;
  /** Zaplanowanie terminu z karty; `override` podmienia podpowiedź (np. rozmowa o przedłużeniu). */
  onSchedule: (deal: DealDetail, override?: { type?: string; title?: string }) => void;
  onMove: (deal: DealDetail, stage: string) => void;
  onDeleted: () => void;
  onClose: () => void;
}

const dateTime = new Intl.DateTimeFormat("pl-PL", {
  day: "numeric",
  month: "short",
  hour: "2-digit",
  minute: "2-digit",
});

/**
 * Panel karty transakcji. Wysuwany z prawej, żeby tablica została widoczna.
 *
 * To tu kalendarz i lejek spotykają się najmocniej: najbliższy krok, przycisk
 * „Zaplanuj" zakładający termin od razu powiązany z transakcją oraz oś
 * wszystkich terminów z rezultatami, czyli zapis tego, jak szła sprzedaż.
 */
export function DealPanel({
  dealId,
  version,
  dictionaries,
  clients,
  label,
  onChanged,
  onEdit,
  onSchedule,
  onMove,
  onDeleted,
  onClose,
}: DealPanelProps) {
  const [deal, setDeal] = useState<DealDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    setLoading(true);
    fetchDeal(dealId)
      .then((result) => {
        setDeal(result);
        setError(null);
      })
      .catch((cause) =>
        setError(
          cause instanceof ApiError ? cause.message : "Nie udało się wczytać transakcji.",
        ),
      )
      .finally(() => setLoading(false));
  }, [dealId, version]);

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape" && !confirmDelete) onClose();
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [onClose, confirmDelete]);

  async function remove() {
    setDeleting(true);
    try {
      await deleteDeal(dealId);
      onDeleted();
    } catch (cause) {
      setError(
        cause instanceof ApiError ? cause.message : "Nie udało się usunąć transakcji.",
      );
      setDeleting(false);
      setConfirmDelete(false);
    }
  }

  const card = deal?.card;
  const closed = card?.closedAt != null;

  return (
    <aside
      className="fixed inset-y-0 right-0 z-30 flex w-full max-w-[420px] flex-col border-l border-line bg-surface"
      aria-label="Szczegóły transakcji"
    >
      <header className="flex items-start justify-between gap-2 border-b border-line px-4 py-3">
        <div className="min-w-0">
          {card && (
            <Badge tone={card.stage === "WON" ? "good" : card.stage === "LOST" ? "critical" : "accent"}>
              {label(card.stage)}
            </Badge>
          )}
          <h2 className="mt-1 text-[14px] font-semibold tracking-tight text-ink">
            {card?.title ?? (loading ? "Wczytywanie…" : "Transakcja")}
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

      {!deal || !card ? (
        <p className={cn("p-4 text-[13px]", error ? "text-critical" : "text-ink-muted")}>
          {error ?? "Wczytywanie…"}
        </p>
      ) : (
        <>
          <div className={cn("flex flex-1 flex-col gap-4 overflow-y-auto p-4", loading && "opacity-60")}>
            {/* Zmiana etapu bez przeciągania. Na telefonie i z klawiatury. */}
            <label className="flex flex-col gap-1.5 text-[13px] font-medium text-ink">
              Etap
              <select
                value={card.stage}
                onChange={(changed) => onMove(deal, changed.target.value)}
                className="h-9 w-full rounded-md border border-line bg-surface px-2.5 text-[13px] font-normal text-ink"
              >
                {dictionaries?.stage.map((entry) => (
                  <option key={entry.value} value={entry.value}>
                    {entry.label}
                  </option>
                ))}
              </select>
              {!closed && (
                <span
                  className={cn(
                    "text-[12px] font-normal",
                    staleness(card) === "dead"
                      ? "text-critical"
                      : staleness(card) === "stale"
                        ? "text-warning-ink"
                        : "text-ink-muted",
                  )}
                >
                  W tym etapie od {daysLabel(daysInStage(card))}
                </span>
              )}
            </label>

            {card.lostReason && (
              <div className="rounded-md border border-critical/30 bg-critical/8 px-2.5 py-2">
                <p className="text-[12px] font-medium text-critical">
                  Przegrana: {label(card.lostReason)}
                </p>
                {deal.lostNote && (
                  <p className="mt-1 whitespace-pre-line text-[12px] text-ink-secondary">
                    {deal.lostNote}
                  </p>
                )}
              </div>
            )}

            {!closed && (
              <section className="flex flex-col gap-2">
                <h3 className="text-[12px] font-semibold tracking-tight text-ink">Następny krok</h3>
                {card.nextEvent ? (
                  <NextStep deal={deal} label={label} />
                ) : (
                  <p className="flex items-center gap-1.5 rounded-md border border-warning/40 bg-warning/12 px-2.5 py-2 text-[12px] font-medium text-warning-ink">
                    <AlertTriangle className="size-3.5 shrink-0" strokeWidth={2} />
                    Nic nie jest zaplanowane. Transakcja stoi.
                  </p>
                )}
                <Button size="sm" variant="secondary" onClick={() => onSchedule(deal)}>
                  <CalendarPlus className="size-3.5" strokeWidth={2} />
                  Zaplanuj termin
                </Button>
              </section>
            )}

            <section className="flex flex-col gap-2">
              {card.propertyId && (
                <Link
                  to={`/nieruchomosci/${card.propertyId}`}
                  className="flex items-start gap-2 rounded-md border border-line px-2.5 py-2 text-[13px] transition-colors hover:border-line-strong hover:bg-subtle"
                >
                  <Building2 className="mt-0.5 size-4 shrink-0 text-ink-muted" strokeWidth={2} />
                  <span className="min-w-0">
                    <span className="block truncate text-ink">{card.propertyAddress ?? "Oferta"}</span>
                    <span className="block text-[12px] text-ink-muted tabular-nums">
                      {card.propertyReference}
                    </span>
                  </span>
                </Link>
              )}
              {card.clientId && (
                <PartyLink id={card.clientId} name={card.clientName} role="właściciel" />
              )}
              {card.buyerId && (
                <PartyLink
                  id={card.buyerId}
                  name={card.buyerName}
                  role={card.transactionType === "RENT" ? "najemca" : "kupujący"}
                />
              )}
            </section>

            <DeadlinesSection
              deal={deal}
              types={dictionaries?.deadlineType ?? []}
              label={label}
              onChanged={(updated) => {
                setDeal(updated);
                onChanged();
              }}
              onScheduleRenewal={(title) => onSchedule(deal, { type: "MEETING", title })}
            />

            <InterestsSection
              deal={deal}
              statuses={dictionaries?.interestStatus ?? []}
              clients={clients}
              label={label}
              onChanged={(updated) => {
                setDeal(updated);
                onChanged();
              }}
            />

            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-[13px]">
              <div>
                <dt className="text-[12px] text-ink-muted">Wartość</dt>
                <dd className="font-medium text-ink tabular-nums">
                  {card.value != null ? formatCurrency(card.value) : "-"}
                </dd>
              </div>
              <div>
                <dt className="text-[12px] text-ink-muted">Prowizja</dt>
                <dd className="font-medium text-ink tabular-nums">
                  {card.commission != null ? formatCurrency(card.commission) : "-"}
                </dd>
              </div>
              <div>
                <dt className="text-[12px] text-ink-muted">Agent</dt>
                <dd className="text-ink">{card.agentName}</dd>
              </div>
              <div>
                <dt className="text-[12px] text-ink-muted">Dodał(a)</dt>
                <dd className="text-ink">{deal.createdByName}</dd>
              </div>
            </dl>

            {deal.notes && (
              <p className="whitespace-pre-line text-[13px] text-ink-secondary">{deal.notes}</p>
            )}

            <section className="flex flex-col gap-2">
              <h3 className="text-[12px] font-semibold tracking-tight text-ink">
                Terminy{" "}
                <span className="font-normal text-ink-muted tabular-nums">{deal.events.length}</span>
              </h3>
              {deal.events.length === 0 ? (
                <p className="text-[12px] text-ink-muted">
                  Brak terminów. Zaplanowane z tej karty pojawią się tu i w kalendarzu.
                </p>
              ) : (
                <ul className="flex flex-col divide-y divide-line rounded-md border border-line">
                  {deal.events.map((event) => {
                    const Icon = eventIcon(event.type);
                    return (
                      <li key={event.id} className="flex items-start gap-2 px-2.5 py-2">
                        <Icon
                          className={cn("mt-0.5 size-3.5 shrink-0", typeInk(event.type))}
                          strokeWidth={2}
                        />
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-[13px] text-ink">{event.title}</p>
                          <p className="text-[12px] text-ink-muted tabular-nums">
                            {formatDayMonth(new Date(event.startsAt))} ·{" "}
                            {timeRange(event.startsAt, event.endsAt, event.allDay)}
                            {event.outcome && (
                              <span className="text-ink-secondary"> · {label(event.outcome)}</span>
                            )}
                          </p>
                        </div>
                        <Badge tone={badgeTone[statusTone(event.status)]}>
                          {label(event.status)}
                        </Badge>
                      </li>
                    );
                  })}
                </ul>
              )}
            </section>

            <section className="flex flex-col gap-2">
              <h3 className="text-[12px] font-semibold tracking-tight text-ink">Historia etapów</h3>
              <ol className="flex flex-col gap-1.5 border-l border-line pl-3">
                {deal.history.map((change, index) => (
                  <li key={index} className="text-[12px] text-ink-secondary">
                    <span className="text-ink">
                      {change.fromStage
                        ? `${label(change.fromStage)} → ${label(change.toStage)}`
                        : `Dodana w etapie „${label(change.toStage)}”`}
                    </span>
                    <span className="block text-ink-muted">
                      {dateTime.format(new Date(change.changedAt))} · {change.changedByName}
                    </span>
                  </li>
                ))}
              </ol>
            </section>

            {error && (
              <p className="rounded-md border border-critical/30 bg-critical/8 px-2.5 py-1.5 text-[12px] text-critical">
                {error}
              </p>
            )}
          </div>

          <footer className="flex items-center gap-2 border-t border-line px-4 py-3">
            <Button size="sm" variant="secondary" onClick={() => onEdit(deal)}>
              <Pencil className="size-3.5" strokeWidth={2} />
              Edytuj
            </Button>
            <Button
              size="sm"
              variant="ghost"
              onClick={() => setConfirmDelete(true)}
              className="ml-auto text-critical hover:bg-critical/8"
            >
              <Trash2 className="size-3.5" strokeWidth={2} />
              Usuń
            </Button>
          </footer>
        </>
      )}

      {confirmDelete && card && (
        <ConfirmDialog
          title="Usunąć tę transakcję?"
          description={
            <>
              Karta „{card.title}” zniknie z tablicy razem z historią etapów. Terminy
              zostaną w kalendarzu. Transakcję, która się nie udała, lepiej oznaczyć
              jako przegraną. Wtedy zostaje w statystykach.
            </>
          }
          busy={deleting}
          onConfirm={remove}
          onCancel={() => setConfirmDelete(false)}
        />
      )}
    </aside>
  );
}

function NextStep({ deal, label }: { deal: DealDetail; label: (value: string) => string }) {
  const next = deal.card.nextEvent!;
  const Icon = eventIcon(next.type);
  return (
    <div className="flex items-start gap-2 rounded-md border border-line bg-subtle px-2.5 py-2">
      <Icon className={cn("mt-0.5 size-4 shrink-0", typeInk(next.type))} strokeWidth={2} />
      <div className="min-w-0">
        <p className="truncate text-[13px] font-medium text-ink">{next.title}</p>
        <p className="text-[12px] text-ink-secondary">
          {label(next.type)} · {whenLabel(next.startsAt, next.allDay)}
          {next.location && ` · ${next.location}`}
        </p>
      </div>
    </div>
  );
}

function PartyLink({ id, name, role }: { id: string; name: string | null; role: string }) {
  return (
    <Link
      to={`/klienci/${id}`}
      className="flex items-center gap-2 rounded-md border border-line px-2.5 py-2 text-[13px] transition-colors hover:border-line-strong hover:bg-subtle"
    >
      <User className="size-4 shrink-0 text-ink-muted" strokeWidth={2} />
      <span className="truncate text-ink">{name}</span>
      <span className="ml-auto shrink-0 text-[11px] text-ink-muted">{role}</span>
    </Link>
  );
}
