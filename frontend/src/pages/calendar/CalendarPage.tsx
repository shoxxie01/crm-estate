import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ChevronLeft, ChevronRight, Plus } from "lucide-react";
import {
  fetchCalendarDictionaries,
  fetchEvents,
  type CalendarDictionaries,
  type EventDetail,
  type EventSummary,
} from "../../api/calendar";
import { fetchClients, type ClientSummary } from "../../api/clients";
import {
  fetchCalendarDeadlines,
  fetchDealDictionaries,
  type CalendarDeadline,
  type DealDictionaries,
} from "../../api/deals";
import { fetchProperties, type PropertySummary } from "../../api/properties";
import { ApiError } from "../../api/client";
import { Button } from "../../components/ui/Button";
import { Select } from "../../components/ui/Select";
import { cn } from "../../lib/cn";
import { CalendarGrid } from "./CalendarGrid";
import { EventForm } from "./EventForm";
import { EventPanel } from "./EventPanel";
import { periodLabel, rangeFor, shift, startOfDay, toDateInput, type CalendarView } from "./dates";
import { typeDot } from "./eventMeta";

const VIEWS: { value: CalendarView; label: string }[] = [
  { value: "month", label: "Miesiąc" },
  { value: "week", label: "Tydzień" },
  { value: "day", label: "Dzień" },
];

/** Godzina, na którą zakładamy termin po kliknięciu w pusty dzień. */
const DEFAULT_HOUR = 10;

export function CalendarPage() {
  const [view, setView] = useState<CalendarView>("month");
  const [anchor, setAnchor] = useState(() => new Date());

  const [mine, setMine] = useState(false);
  const [type, setType] = useState("");
  const [status, setStatus] = useState("");

  const navigate = useNavigate();
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [deadlines, setDeadlines] = useState<CalendarDeadline[]>([]);
  const [dictionaries, setDictionaries] = useState<CalendarDictionaries | null>(null);
  const [properties, setProperties] = useState<PropertySummary[]>([]);
  const [clients, setClients] = useState<ClientSummary[]>([]);
  const [dealDictionaries, setDealDictionaries] = useState<DealDictionaries | null>(null);

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [editing, setEditing] = useState<EventDetail | null>(null);
  const [formDay, setFormDay] = useState<Date | null>(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [from, to] = useMemo(() => rangeFor(view, anchor), [view, anchor]);

  const reload = useCallback(() => {
    setLoading(true);
    fetchEvents(from, to, { mine, type: type || undefined, status: status || undefined })
      .then((result) => {
        setEvents(result);
        setError(null);
      })
      .catch((cause) =>
        setError(
          cause instanceof ApiError ? cause.message : "Nie udało się wczytać kalendarza.",
        ),
      )
      .finally(() => setLoading(false));
  }, [from, to, mine, type, status]);

  useEffect(() => {
    reload();
  }, [reload]);

  // Terminy umowne to osobna warstwa z modułu transakcji. Filtry rodzaju
  // i statusu dotyczą spotkań, więc przy nich warstwę chowamy. Inaczej
  // „tylko prezentacje" pokazywałoby też koniec umowy pośrednictwa.
  const showDeadlines = !type && !status;
  useEffect(() => {
    if (!showDeadlines) {
      setDeadlines([]);
      return;
    }
    fetchCalendarDeadlines(toDateInput(from), toDateInput(to), mine)
      .then(setDeadlines)
      .catch(() => setDeadlines([]));
  }, [from, to, mine, showDeadlines]);

  // Słowniki i listy do formularza. Raz na wejście w moduł.
  useEffect(() => {
    fetchCalendarDictionaries().then(setDictionaries).catch(() => undefined);
    fetchProperties().then((page) => setProperties(page.content)).catch(() => undefined);
    fetchClients().then((page) => setClients(page.content)).catch(() => undefined);
    // Statusy uczestników terminu (zainteresowanych z transakcji).
    fetchDealDictionaries().then(setDealDictionaries).catch(() => undefined);
  }, []);

  const labels = useMemo(() => {
    const map = new Map<string, string>();
    if (!dictionaries) return map;
    const groups = [dictionaries.type, dictionaries.status, dictionaries.outcome];
    if (dealDictionaries) {
      groups.push(dealDictionaries.interestStatus, dealDictionaries.stage, dealDictionaries.deadlineType);
    }
    for (const group of groups) {
      for (const entry of group) map.set(entry.value, entry.label);
    }
    return map;
  }, [dictionaries, dealDictionaries]);

  const label = (value: string) => labels.get(value) ?? value;

  function openNewEvent(day: Date) {
    const at = startOfDay(day);
    at.setHours(DEFAULT_HOUR, 0, 0, 0);
    setEditing(null);
    setFormDay(at);
  }

  return (
    <div className="flex flex-col gap-4">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-base font-semibold tracking-tight text-ink">Kalendarz</h1>
          <p className="mt-0.5 text-[13px] text-ink-secondary first-letter:uppercase">
            {periodLabel(view, anchor)}
            {!loading && ` · ${events.length} ${events.length === 1 ? "termin" : "terminów"}`}
          </p>
        </div>

        <div className="flex flex-wrap items-end gap-2">
          <Select
            label="Rodzaj"
            placeholder="Wszystkie"
            options={dictionaries?.type ?? []}
            value={type}
            onChange={(changed) => setType(changed.target.value)}
            className="w-40"
          />
          <Select
            label="Status"
            placeholder="Wszystkie"
            options={dictionaries?.status ?? []}
            value={status}
            onChange={(changed) => setStatus(changed.target.value)}
            className="w-36"
          />
          <Button
            variant={mine ? "primary" : "secondary"}
            onClick={() => setMine((current) => !current)}
            aria-pressed={mine}
          >
            Tylko moje
          </Button>
          <Button onClick={() => openNewEvent(new Date())}>
            <Plus className="size-4" strokeWidth={2} />
            Nowy termin
          </Button>
        </div>
      </header>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-1">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setAnchor((current) => shift(view, current, -1))}
            aria-label="Poprzedni okres"
          >
            <ChevronLeft className="size-4" strokeWidth={2} />
          </Button>
          <Button variant="secondary" size="sm" onClick={() => setAnchor(new Date())}>
            Dziś
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setAnchor((current) => shift(view, current, 1))}
            aria-label="Następny okres"
          >
            <ChevronRight className="size-4" strokeWidth={2} />
          </Button>
        </div>

        <div
          className="flex rounded-md border border-line bg-surface p-0.5"
          role="group"
          aria-label="Zakres widoku"
        >
          {VIEWS.map((option) => (
            <button
              key={option.value}
              onClick={() => setView(option.value)}
              aria-pressed={view === option.value}
              className={cn(
                "rounded px-2.5 py-1 text-[12px] transition-colors",
                view === option.value
                  ? "bg-subtle font-medium text-ink"
                  : "text-ink-secondary hover:text-ink",
              )}
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>

      {/*
        Legenda kolorów rodzaju. Etykiety biorą się ze słownika z backendu, więc
        dołożenie rodzaju po stronie serwera nie wymaga ruszania tej listy.
        Nowa pozycja dostanie kolor „Inne", dopóki nie dopiszemy jej do palety.
      */}
      {dictionaries && (
        <ul className="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-[12px] text-ink-secondary">
          {dictionaries.type.map((entry) => (
            <li key={entry.value} className="flex items-center gap-1.5">
              <span
                className={cn("size-2 shrink-0 rounded-full", typeDot(entry.value))}
                aria-hidden
              />
              {entry.label}
            </li>
          ))}
        </ul>
      )}

      {error && (
        <p className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {error}
        </p>
      )}

      <div
        className={cn(
          "grid gap-4",
          selectedId ? "lg:grid-cols-[minmax(0,1fr)_340px]" : "grid-cols-1",
        )}
      >
        <div className={cn(loading && "opacity-60 transition-opacity")}>
          <CalendarGrid
            view={view}
            anchor={anchor}
            events={events}
            selectedId={selectedId}
            onSelect={(event) => setSelectedId(event.id)}
            onCreateAt={openNewEvent}
            deadlines={deadlines}
            onOpenDeadline={(deadline) => navigate(`/kanban?karta=${deadline.dealId}`)}
            label={label}
          />
        </div>

        {selectedId && (
          <EventPanel
            eventId={selectedId}
            dictionaries={dictionaries}
            label={label}
            onEdit={(event) => {
              setEditing(event);
              setFormDay(new Date(event.summary.startsAt));
            }}
            onChanged={reload}
            onClose={() => setSelectedId(null)}
          />
        )}
      </div>

      {formDay && dictionaries && (
        <EventForm
          dictionaries={dictionaries}
          initial={editing}
          defaultDay={formDay}
          properties={properties}
          clients={clients}
          onSaved={(saved) => {
            setFormDay(null);
            setEditing(null);
            reload();
            // Termin zamknięty z formularza też może podpowiadać etap transakcji.
            // Pokazujemy go w panelu, żeby propozycja nie przepadła po zapisie.
            if (saved.suggestion) setSelectedId(saved.summary.id);
          }}
          onClose={() => {
            setFormDay(null);
            setEditing(null);
          }}
        />
      )}
    </div>
  );
}
