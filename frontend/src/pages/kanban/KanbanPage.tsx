import { useCallback, useEffect, useMemo, useState, type DragEvent } from "react";
import { useSearchParams } from "react-router-dom";
import { Plus } from "lucide-react";
import {
  fetchCalendarDictionaries,
  type CalendarDictionaries,
} from "../../api/calendar";
import { ApiError } from "../../api/client";
import { fetchClients, type ClientSummary } from "../../api/clients";
import {
  CLOSED_STAGES,
  changeDealStage,
  fetchBoard,
  fetchDealDictionaries,
  type DealCard,
  type DealDetail,
  type DealDictionaries,
} from "../../api/deals";
import { fetchProperties, type PropertySummary } from "../../api/properties";
import { Button } from "../../components/ui/Button";
import { cn } from "../../lib/cn";
import { formatCompactPLN } from "../../lib/format";
import { EventForm, type EventPreset } from "../calendar/EventForm";
import { DealBoardCard } from "./DealBoardCard";
import { DealForm } from "./DealForm";
import { DealPanel } from "./DealPanel";
import { KanbanTabs } from "./KanbanTabs";
import { LostDialog } from "./LostDialog";
import { daysUntil, suggestedEventType } from "./dealMeta";

/** Godzina, na którą podpowiadamy termin zakładany z karty. Jutro rano. */
const DEFAULT_HOUR = 10;

interface PendingLoss {
  card: DealCard;
}

/**
 * Tablica Kanban transakcji. Lejek sprzedaży biura.
 *
 * Kolumny to etapy ze słownika backendu, w jego kolejności. Kartę przenosi się
 * przeciągnięciem albo z panelu (lista „Etap"). To drugie działa też na
 * telefonie i z klawiatury, gdzie przeciąganie nie istnieje.
 *
 * Z kalendarzem tablica łączy się w dwie strony: karta pokazuje najbliższy
 * termin (albo ostrzega, że go nie ma), a „Zaplanuj termin" w panelu otwiera
 * ten sam formularz co kalendarz, z wypełnioną ofertą, klientem i transakcją.
 */
export function KanbanPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedId = searchParams.get("karta");

  const [cards, setCards] = useState<DealCard[]>([]);
  const [dictionaries, setDictionaries] = useState<DealDictionaries | null>(null);
  const [calendarDictionaries, setCalendarDictionaries] =
    useState<CalendarDictionaries | null>(null);
  const [properties, setProperties] = useState<PropertySummary[]>([]);
  const [clients, setClients] = useState<ClientSummary[]>([]);

  const [mine, setMine] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [draggedId, setDraggedId] = useState<string | null>(null);
  const [overStage, setOverStage] = useState<string | null>(null);
  const [pendingLoss, setPendingLoss] = useState<PendingLoss | null>(null);
  const [lossBusy, setLossBusy] = useState(false);
  const [lossError, setLossError] = useState<string | null>(null);

  const [form, setForm] = useState<{ initial: DealDetail | null; stage: string } | null>(null);
  const [scheduleFor, setScheduleFor] = useState<{
    deal: DealDetail;
    override?: { type?: string; title?: string };
  } | null>(null);
  const [panelVersion, setPanelVersion] = useState(0);

  const reload = useCallback(() => {
    setLoading(true);
    fetchBoard({ mine })
      .then((result) => {
        setCards(result);
        setError(null);
      })
      .catch((cause) =>
        setError(
          cause instanceof ApiError ? cause.message : "Nie udało się wczytać tablicy.",
        ),
      )
      .finally(() => setLoading(false));
  }, [mine]);

  useEffect(() => {
    reload();
  }, [reload]);

  useEffect(() => {
    fetchDealDictionaries().then(setDictionaries).catch(() => undefined);
    fetchCalendarDictionaries().then(setCalendarDictionaries).catch(() => undefined);
    fetchProperties().then((page) => setProperties(page.content)).catch(() => undefined);
    fetchClients().then((page) => setClients(page.content)).catch(() => undefined);
  }, []);

  /** Etykiety z obu słowników. Karta pokazuje etapy i rodzaje terminów. */
  const labels = useMemo(() => {
    const map = new Map<string, string>();
    const groups = [
      ...(dictionaries ? [
            dictionaries.stage,
            dictionaries.lostReason,
            dictionaries.interestStatus,
            dictionaries.deadlineType,
          ] : []),
      ...(calendarDictionaries
        ? [calendarDictionaries.type, calendarDictionaries.status, calendarDictionaries.outcome]
        : []),
    ];
    for (const group of groups) {
      for (const entry of group) map.set(entry.value, entry.label);
    }
    return map;
  }, [dictionaries, calendarDictionaries]);

  const label = useCallback((value: string) => labels.get(value) ?? value, [labels]);

  const columns = useMemo(() => {
    const byStage = new Map<string, DealCard[]>();
    for (const card of cards) {
      const list = byStage.get(card.stage) ?? [];
      list.push(card);
      byStage.set(card.stage, list);
    }
    return (dictionaries?.stage ?? []).map((entry) => {
      const stageCards = byStage.get(entry.value) ?? [];
      return {
        stage: entry.value,
        label: entry.label,
        cards: stageCards,
        value: stageCards.reduce((sum, card) => sum + (card.value ?? 0), 0),
      };
    });
  }, [cards, dictionaries]);

  const openColumnCount = columns.filter(
    (column) => !CLOSED_STAGES.includes(column.stage),
  ).length;
  // Szerokość widocznego obszaru (100cqw) minus odstępy (gap-2 = 0,5rem)
  // podzielona po równo między otwarte etapy.
  const openColumnWidth = `calc((100cqw - ${Math.max(0, openColumnCount - 1)} * 0.5rem) / ${Math.max(1, openColumnCount)})`;

  const open = cards.filter((card) => !CLOSED_STAGES.includes(card.stage));
  const pipelineValue = open.reduce((sum, card) => sum + (card.value ?? 0), 0);
  const pipelineCommission = open.reduce((sum, card) => sum + (card.commission ?? 0), 0);
  const withoutNextStep = open.filter((card) => !card.nextEvent).length;
  // Karty, których najbliższy termin umowny wypada w ciągu tygodnia (albo już minął).
  const deadlinesThisWeek = open.filter(
    (card) => card.nextDeadline && daysUntil(card.nextDeadline.dueDate) <= 7,
  ).length;

  function select(id: string | null) {
    setSearchParams(
      (current) => {
        const next = new URLSearchParams(current);
        if (id) next.set("karta", id);
        else next.delete("karta");
        return next;
      },
      { replace: true },
    );
  }

  const refreshAll = useCallback(() => {
    reload();
    setPanelVersion((version) => version + 1);
  }, [reload]);

  /**
   * Przeniesienie karty. Na tablicy zmiana jest widoczna od razu. Czekanie
   * na serwer przy każdym przeciągnięciu sprawiałoby wrażenie, że karta
   * „odskakuje". Przy błędzie wracamy do stanu z serwera.
   */
  async function move(card: DealCard, stage: string) {
    if (card.stage === stage) return;
    if (stage === "LOST") {
      setLossError(null);
      setPendingLoss({ card });
      return;
    }

    setCards((current) =>
      current.map((existing) =>
        existing.id === card.id
          ? { ...existing, stage, stageChangedAt: new Date().toISOString() }
          : existing,
      ),
    );
    try {
      await changeDealStage(card.id, { stage });
      refreshAll();
    } catch (cause) {
      setError(
        cause instanceof ApiError ? cause.message : "Nie udało się przenieść karty.",
      );
      reload();
    }
  }

  async function confirmLoss(reason: string, note: string) {
    if (!pendingLoss) return;
    setLossBusy(true);
    try {
      await changeDealStage(pendingLoss.card.id, {
        stage: "LOST",
        lostReason: reason,
        lostNote: note.trim() || undefined,
      });
      setPendingLoss(null);
      refreshAll();
    } catch (cause) {
      setLossError(
        cause instanceof ApiError ? cause.message : "Nie udało się zapisać przegranej.",
      );
    } finally {
      setLossBusy(false);
    }
  }

  function onDragStart(card: DealCard) {
    return (event: DragEvent<HTMLElement>) => {
      event.dataTransfer.effectAllowed = "move";
      event.dataTransfer.setData("text/plain", card.id);
      setDraggedId(card.id);
    };
  }

  function onDrop(stage: string) {
    return (event: DragEvent<HTMLElement>) => {
      event.preventDefault();
      const id = event.dataTransfer.getData("text/plain") || draggedId;
      const card = cards.find((candidate) => candidate.id === id);
      setDraggedId(null);
      setOverStage(null);
      if (card) void move(card, stage);
    };
  }

  /** Domyślny start terminu planowanego z karty: jutro o 10:00. */
  function schedulePreset(
    deal: DealDetail,
    override?: { type?: string; title?: string },
  ): { day: Date; preset: EventPreset } {
    const day = new Date();
    day.setDate(day.getDate() + 1);
    day.setHours(DEFAULT_HOUR, 0, 0, 0);
    return {
      day,
      preset: {
        type: override?.type ?? suggestedEventType(deal.card.stage),
        title: override?.title,
        propertyId: deal.card.propertyId,
        // W negocjacjach spotyka się zwykle z kupującym, ale klient w terminie
        // to strona podaży (patrz CalendarEvent). Kupującego agent dopisze sam.
        clientId: deal.card.clientId,
        dealId: deal.card.id,
        dealTitle: deal.card.title,
      },
    };
  }

  const pendingSchedule = scheduleFor
    ? schedulePreset(scheduleFor.deal, scheduleFor.override)
    : null;

  return (
    <div className="flex flex-col gap-4">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-base font-semibold tracking-tight text-ink">Kanban</h1>
          <div className="mt-2">
            <KanbanTabs />
          </div>
          <p className="mt-2 text-[13px] text-ink-secondary">
            {loading && cards.length === 0 ? (
              "Wczytywanie…"
            ) : (
              <>
                <span className="tabular-nums">{open.length}</span>{" "}
                {open.length === 1 ? "otwarta transakcja" : "otwartych transakcji"} · w lejku{" "}
                <span className="font-medium text-ink tabular-nums">
                  {formatCompactPLN(pipelineValue)}
                </span>
                {pipelineCommission > 0 && (
                  <>
                    {" "}· prowizje{" "}
                    <span className="font-medium text-ink tabular-nums">
                      {formatCompactPLN(pipelineCommission)}
                    </span>
                  </>
                )}
                {deadlinesThisWeek > 0 && (
                  <span className="font-medium text-critical">
                    {" "}· {deadlinesThisWeek}{" "}
                    {deadlinesThisWeek === 1 ? "termin umowny" : "terminy umowne"} w ciągu 7 dni
                  </span>
                )}
                {withoutNextStep > 0 && (
                  <span className="text-warning-ink">
                    {" "}· {withoutNextStep} bez następnego kroku
                  </span>
                )}
              </>
            )}
          </p>
        </div>

        <div className="flex flex-wrap items-end gap-2">
          <Button
            variant={mine ? "primary" : "secondary"}
            onClick={() => setMine((current) => !current)}
            aria-pressed={mine}
          >
            Tylko moje
          </Button>
          <Button
            onClick={() => setForm({ initial: null, stage: "LEAD" })}
            disabled={!dictionaries}
          >
            <Plus className="size-4" strokeWidth={2} />
            Nowa transakcja
          </Button>
        </div>
      </header>

      {error && (
        <p className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {error}
        </p>
      )}

      {/*
        Otwarte etapy dzielą między siebie całą widoczną szerokość, żeby cały
        lejek mieścił się na ekranie. Wygrana i Przegrana stoją dalej, za
        krawędzią. To archiwum ostatnich 30 dni, nie praca na dziś, więc
        dojeżdża się do nich suwakiem. Poniżej 10,5rem na kolumnę karty
        przestają być czytelne. Wtedy przewijają się także otwarte etapy.
      */}
      <div
        className={cn(
          "@container -mx-4 overflow-x-auto px-4 pb-2 sm:mx-0 sm:px-0",
          loading && "opacity-60 transition-opacity",
        )}
      >
        <div className="flex min-h-[60vh] gap-2">
          {columns.map((column) => {
            const closed = CLOSED_STAGES.includes(column.stage);
            const over = overStage === column.stage && draggedId !== null;
            return (
              <section
                key={column.stage}
                aria-label={column.label}
                onDragOver={(event) => {
                  event.preventDefault();
                  event.dataTransfer.dropEffect = "move";
                  if (overStage !== column.stage) setOverStage(column.stage);
                }}
                onDragLeave={(event) => {
                  if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
                    setOverStage(null);
                  }
                }}
                onDrop={onDrop(column.stage)}
                style={closed ? undefined : { width: openColumnWidth }}
                className={cn(
                  "flex shrink-0 flex-col rounded-lg border transition-colors",
                  closed ? "w-56" : "min-w-[10.5rem]",
                  over
                    ? "border-accent bg-accent-subtle"
                    : closed
                      ? "border-line bg-subtle/60"
                      : "border-line bg-subtle",
                )}
              >
                <header className="flex items-center justify-between gap-1 px-2.5 pt-2 pb-1.5">
                  <div className="min-w-0">
                    <h2
                      className="flex items-center gap-1.5 text-[12px] font-semibold tracking-tight text-ink"
                      title={column.label}
                    >
                      <span
                        className={cn(
                          "size-2 shrink-0 rounded-full",
                          column.stage === "WON"
                            ? "bg-good"
                            : column.stage === "LOST"
                              ? "bg-critical"
                              : "bg-accent",
                        )}
                        aria-hidden
                      />
                      <span className="truncate">{column.label}</span>
                      <span className="font-normal text-ink-muted tabular-nums">
                        {column.cards.length}
                      </span>
                    </h2>
                    <p className="text-[11px] text-ink-muted tabular-nums">
                      {column.value > 0 ? formatCompactPLN(column.value) : "-"}
                      {closed && " · ostatnie 30 dni"}
                    </p>
                  </div>
                  {!closed && (
                    <button
                      type="button"
                      onClick={() => setForm({ initial: null, stage: column.stage })}
                      aria-label={`Dodaj transakcję w etapie ${column.label}`}
                      className="shrink-0 rounded p-0.5 text-ink-muted hover:bg-surface hover:text-ink"
                    >
                      <Plus className="size-4" strokeWidth={2} />
                    </button>
                  )}
                </header>

                <div className="flex flex-1 flex-col gap-1.5 px-1.5 pb-1.5">
                  {column.cards.map((card) => (
                    <DealBoardCard
                      key={card.id}
                      card={card}
                      selected={card.id === selectedId}
                      label={label}
                      onOpen={() => select(card.id)}
                      onDragStart={onDragStart(card)}
                      onDragEnd={() => {
                        setDraggedId(null);
                        setOverStage(null);
                      }}
                    />
                  ))}
                  {column.cards.length === 0 && (
                    <p className="rounded-md border border-dashed border-line-strong px-2 py-3 text-center text-[11px] text-ink-muted">
                      {closed ? "Brak w ostatnim miesiącu" : "Przeciągnij tu kartę"}
                    </p>
                  )}
                </div>
              </section>
            );
          })}
        </div>
      </div>

      {selectedId && (
        <DealPanel
          dealId={selectedId}
          version={panelVersion}
          dictionaries={dictionaries}
          clients={clients}
          label={label}
          onChanged={reload}
          onEdit={(deal) => setForm({ initial: deal, stage: deal.card.stage })}
          onSchedule={(deal, override) => setScheduleFor({ deal, override })}
          onMove={(deal, stage) => void move(deal.card, stage)}
          onDeleted={() => {
            select(null);
            reload();
          }}
          onClose={() => select(null)}
        />
      )}

      {form && dictionaries && (
        <DealForm
          dictionaries={dictionaries}
          initial={form.initial}
          defaultStage={form.stage}
          properties={properties}
          clients={clients}
          onSaved={(saved) => {
            setForm(null);
            select(saved.card.id);
            refreshAll();
          }}
          onClose={() => setForm(null)}
        />
      )}

      {pendingSchedule && calendarDictionaries && (
        <EventForm
          dictionaries={calendarDictionaries}
          initial={null}
          defaultDay={pendingSchedule.day}
          properties={properties}
          clients={clients}
          preset={pendingSchedule.preset}
          onSaved={() => {
            setScheduleFor(null);
            refreshAll();
          }}
          onClose={() => setScheduleFor(null)}
        />
      )}

      {pendingLoss && dictionaries && (
        <LostDialog
          title={pendingLoss.card.title}
          reasons={dictionaries.lostReason}
          busy={lossBusy}
          error={lossError}
          onConfirm={confirmLoss}
          onCancel={() => setPendingLoss(null)}
        />
      )}
    </div>
  );
}
