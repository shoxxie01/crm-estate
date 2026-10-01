import { FileText } from "lucide-react";
import type { EventSummary } from "../../api/calendar";
import type { CalendarDeadline } from "../../api/deals";
import { cn } from "../../lib/cn";
import {
  addDays,
  formatDayLong,
  formatDayShort,
  sameDay,
  startOfDay,
  startOfMonthGrid,
  startOfWeek,
  timeRange,
  toDateInput,
  type CalendarView,
} from "./dates";
import { eventIcon, statusChip, typeChip, typeInk } from "./eventMeta";

interface GridProps {
  view: CalendarView;
  anchor: Date;
  events: EventSummary[];
  selectedId: string | null;
  onSelect: (event: EventSummary) => void;
  /** Kliknięcie w pustą część dnia. Zakłada termin na tej dacie. */
  onCreateAt: (day: Date) => void;
  /** Terminy umowne transakcji. Warstwa tylko do odczytu, nad terminami dnia. */
  deadlines: CalendarDeadline[];
  /** Kliknięcie terminu umownego. Prowadzi do karty transakcji. */
  onOpenDeadline: (deadline: CalendarDeadline) => void;
  label: (value: string) => string;
}

/** Ile kafelków mieści komórka miesiąca, zanim zacznie zwijać resztę w „+N". */
const MONTH_CHIP_LIMIT = 3;

export function CalendarGrid(props: GridProps) {
  if (props.view === "day") return <DayAgenda {...props} />;
  if (props.view === "week") return <WeekGrid {...props} />;
  return <MonthGrid {...props} />;
}

/** Kafelek terminu. Rodzaj niesie kolor i ikona, status. Sposób podania (patrz eventMeta). */
function EventChip({
  event,
  selected,
  onSelect,
  dense,
}: {
  event: EventSummary;
  selected: boolean;
  onSelect: (event: EventSummary) => void;
  dense?: boolean;
}) {
  const Icon = eventIcon(event.type);

  return (
    <button
      type="button"
      onClick={(clickEvent) => {
        clickEvent.stopPropagation();
        onSelect(event);
      }}
      aria-current={selected ? "true" : undefined}
      className={cn(
        "flex w-full items-center gap-1.5 rounded border border-l-[3px] px-1.5 text-left text-ink transition hover:brightness-95",
        dense ? "py-0.5 text-[11px]" : "py-1 text-[12px]",
        typeChip(event.type),
        statusChip(event.status),
        selected && "ring-2 ring-accent-ring",
      )}
      title={`${event.title} · ${timeRange(event.startsAt, event.endsAt, event.allDay)}`}
    >
      <Icon className={cn("size-3 shrink-0", typeInk(event.type))} strokeWidth={2} aria-hidden />
      {!event.allDay && (
        <span className="shrink-0 tabular-nums text-ink-secondary">
          {timeRange(event.startsAt, event.endsAt, false).split("–")[0]}
        </span>
      )}
      <span className="truncate">{event.title}</span>
    </button>
  );
}

/**
 * Termin umowny w siatce. Celowo inny niż kafelek spotkania. Przerywana ramka
 * i ikona dokumentu. Bo to nie jest wpis, który się odbywa, tylko data z umowy.
 */
function DeadlineChip({
  deadline,
  label,
  onOpen,
  dense,
}: {
  deadline: CalendarDeadline;
  label: (value: string) => string;
  onOpen: (deadline: CalendarDeadline) => void;
  dense?: boolean;
}) {
  const met = deadline.status === "MET";
  return (
    <button
      type="button"
      onClick={(clickEvent) => {
        clickEvent.stopPropagation();
        onOpen(deadline);
      }}
      className={cn(
        "flex w-full items-center gap-1.5 rounded border border-dashed border-line-strong bg-surface px-1.5 text-left transition hover:bg-subtle",
        dense ? "py-0.5 text-[11px]" : "py-1 text-[12px]",
        met ? "text-ink-muted line-through" : "font-medium text-ink",
      )}
      title={`${label(deadline.type)}. ${deadline.dealTitle}${met ? " (dotrzymany)" : ""}`}
    >
      <FileText className="size-3 shrink-0 text-ink-secondary" strokeWidth={2} aria-hidden />
      <span className="truncate">
        {label(deadline.type)} · {deadline.dealTitle}
      </span>
    </button>
  );
}

function deadlinesOn(deadlines: CalendarDeadline[], day: Date): CalendarDeadline[] {
  const date = toDateInput(day);
  return deadlines.filter((deadline) => deadline.dueDate === date);
}

function eventsOn(events: EventSummary[], day: Date): EventSummary[] {
  const from = startOfDay(day);
  const to = addDays(from, 1);

  // Termin wielodniowy ma się pokazać w każdym dniu, na który zachodzi.
  // Stąd przecięcie zakresów, a nie porównanie samej daty początku.
  return events.filter((event) => {
    const starts = new Date(event.startsAt);
    const ends = new Date(event.endsAt);
    return starts < to && ends > from;
  });
}

function MonthGrid({
  anchor,
  events,
  selectedId,
  onSelect,
  onCreateAt,
  deadlines,
  onOpenDeadline,
  label,
}: GridProps) {
  const first = startOfMonthGrid(anchor);
  const days = Array.from({ length: 42 }, (_, index) => addDays(first, index));
  const today = new Date();

  return (
    <div className="card overflow-hidden">
      <div className="grid grid-cols-7 border-b border-line bg-subtle">
        {days.slice(0, 7).map((day) => (
          <div
            key={day.toISOString()}
            className="px-2 py-1.5 text-[11px] font-medium text-ink-secondary first-letter:uppercase"
          >
            {formatDayShort(day)}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7">
        {days.map((day) => {
          const dayEvents = eventsOn(events, day);
          const dayDeadlines = deadlinesOn(deadlines, day);
          // Terminy umowne mają pierwszeństwo. Zajmują miejsca w komórce przed spotkaniami.
          const eventSlots = Math.max(0, MONTH_CHIP_LIMIT - dayDeadlines.length);
          const hidden = Math.max(0, dayEvents.length - eventSlots);
          const outside = day.getMonth() !== anchor.getMonth();
          const isToday = sameDay(day, today);

          return (
            <div
              key={day.toISOString()}
              onClick={() => onCreateAt(day)}
              className={cn(
                "min-h-[104px] cursor-pointer border-r border-b border-line p-1.5 last:border-r-0",
                "transition-colors hover:bg-subtle/60",
                outside && "bg-canvas",
              )}
            >
              <div className="mb-1 flex items-center justify-between">
                <span
                  className={cn(
                    "inline-flex size-5 items-center justify-center rounded text-[11px] tabular-nums",
                    outside ? "text-ink-muted" : "text-ink-secondary",
                    isToday && "bg-accent font-semibold text-white",
                  )}
                >
                  {day.getDate()}
                </span>
              </div>

              <div className="flex flex-col gap-0.5">
                {dayDeadlines.map((deadline) => (
                  <DeadlineChip
                    key={deadline.id}
                    deadline={deadline}
                    label={label}
                    onOpen={onOpenDeadline}
                    dense
                  />
                ))}
                {dayEvents.slice(0, eventSlots).map((event) => (
                  <EventChip
                    key={event.id}
                    event={event}
                    selected={event.id === selectedId}
                    onSelect={onSelect}
                    dense
                  />
                ))}
                {hidden > 0 && (
                  <span className="px-1 text-[11px] text-ink-muted">+{hidden} więcej</span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function WeekGrid({
  anchor,
  events,
  selectedId,
  onSelect,
  onCreateAt,
  deadlines,
  onOpenDeadline,
  label,
}: GridProps) {
  const first = startOfWeek(anchor);
  const days = Array.from({ length: 7 }, (_, index) => addDays(first, index));
  const today = new Date();

  return (
    <div className="card grid grid-cols-7 overflow-hidden">
      {days.map((day) => {
        const dayEvents = eventsOn(events, day);
        const dayDeadlines = deadlinesOn(deadlines, day);
        const isToday = sameDay(day, today);

        return (
          <div
            key={day.toISOString()}
            onClick={() => onCreateAt(day)}
            className="min-h-[420px] cursor-pointer border-r border-line last:border-r-0 hover:bg-subtle/40"
          >
            <div
              className={cn(
                "flex items-baseline gap-1.5 border-b px-2 py-1.5",
                isToday ? "border-accent bg-accent-subtle" : "border-line bg-subtle",
              )}
            >
              <span className="text-[11px] text-ink-secondary first-letter:uppercase">
                {formatDayShort(day)}
              </span>
              <span
                className={cn(
                  "text-[13px] tabular-nums",
                  isToday ? "font-semibold text-accent" : "text-ink",
                )}
              >
                {day.getDate()}
              </span>
            </div>

            <div className="flex flex-col gap-1 p-1.5">
              {dayDeadlines.map((deadline) => (
                <DeadlineChip
                  key={deadline.id}
                  deadline={deadline}
                  label={label}
                  onOpen={onOpenDeadline}
                />
              ))}
              {dayEvents.map((event) => (
                <EventChip
                  key={event.id}
                  event={event}
                  selected={event.id === selectedId}
                  onSelect={onSelect}
                />
              ))}
              {dayEvents.length === 0 && dayDeadlines.length === 0 && (
                <span className="px-1 py-2 text-[11px] text-ink-muted">-</span>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

/**
 * Widok dnia jest listą, nie siatką godzin. Terminy w biurze nieruchomości są
 * rozrzucone po całym dniu, więc oś godzinowa to w praktyce ekran pustych
 * wierszy. Lista daje ten sam porządek bez przewijania przez pustkę.
 */
function DayAgenda({
  anchor,
  events,
  selectedId,
  onSelect,
  onCreateAt,
  deadlines,
  onOpenDeadline,
  label,
}: GridProps) {
  const dayEvents = eventsOn(events, anchor);
  const dayDeadlines = deadlinesOn(deadlines, anchor);

  return (
    <div className="card flex flex-col">
      <header className="flex items-center justify-between border-b border-line px-4 py-2.5">
        <h2 className="text-[13px] font-semibold text-ink first-letter:uppercase">
          {formatDayLong(anchor)}
        </h2>
        <span className="text-[12px] text-ink-muted">
          {dayEvents.length === 0
            ? "brak terminów"
            : `${dayEvents.length} ${dayEvents.length === 1 ? "termin" : "terminy/-ów"}`}
        </span>
      </header>

      {dayDeadlines.length > 0 && (
        <div className="flex flex-col gap-1 border-b border-line px-4 py-2.5">
          <p className="text-[11px] font-medium text-ink-muted">Terminy umowne</p>
          {dayDeadlines.map((deadline) => (
            <DeadlineChip
              key={deadline.id}
              deadline={deadline}
              label={label}
              onOpen={onOpenDeadline}
            />
          ))}
        </div>
      )}

      {dayEvents.length === 0 ? (
        <button
          type="button"
          onClick={() => onCreateAt(anchor)}
          className="px-4 py-10 text-[13px] text-ink-muted hover:text-ink"
        >
          Kliknij, aby dodać termin na ten dzień.
        </button>
      ) : (
        <ul className="divide-y divide-line">
          {dayEvents.map((event) => {
            const Icon = eventIcon(event.type);
            return (
              <li key={event.id}>
                <button
                  type="button"
                  onClick={() => onSelect(event)}
                  className={cn(
                    "flex w-full items-start gap-3 px-4 py-2.5 text-left transition-colors hover:bg-subtle",
                    event.id === selectedId && "bg-accent-subtle",
                  )}
                >
                  <span className="w-24 shrink-0 pt-0.5 text-[12px] tabular-nums text-ink-secondary">
                    {timeRange(event.startsAt, event.endsAt, event.allDay)}
                  </span>
                  <Icon
                    className={cn("mt-0.5 size-4 shrink-0", typeInk(event.type))}
                    strokeWidth={2}
                    aria-hidden
                  />
                  <span className="min-w-0">
                    <span
                      className={cn(
                        "block truncate text-[13px] text-ink",
                        event.status === "CANCELLED" && "text-ink-muted line-through",
                      )}
                    >
                      {event.title}
                    </span>
                    <span className="block truncate text-[12px] text-ink-muted">
                      {[event.location, event.clientName ?? event.counterpartyName, event.agentName]
                        .filter(Boolean)
                        .join(" · ")}
                    </span>
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
