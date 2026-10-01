import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  fetchCalendarDictionaries,
  fetchClientEvents,
  fetchPropertyEvents,
  type EventSummary,
} from "../../api/calendar";
import { Badge } from "../../components/ui/Badge";
import { cn } from "../../lib/cn";
import { formatDayMonth, formatTime, timeRange } from "./dates";
import { badgeTone, eventIcon, statusTone, typeInk } from "./eventMeta";

/**
 * Terminy związane z ofertą albo z klientem. Sekcja „Terminy" na ich kartach.
 *
 * <p>To jest właściwe spięcie kalendarza z resztą CRM-u: z karty oferty widać
 * całą historię pokazów razem z rezultatami, więc rozmowa z właścicielem
 * o obniżce ceny opiera się na zapisanych faktach, a nie na pamięci agenta.
 */
export function EventTimeline({
  propertyId,
  clientId,
}: {
  propertyId?: string;
  clientId?: string;
}) {
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [labels, setLabels] = useState<Map<string, string>>(new Map());
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const request = propertyId
      ? fetchPropertyEvents(propertyId)
      : clientId
        ? fetchClientEvents(clientId)
        : null;

    if (!request) return;

    setLoading(true);
    request
      .then(setEvents)
      .catch(() => setEvents([]))
      .finally(() => setLoading(false));
  }, [propertyId, clientId]);

  useEffect(() => {
    fetchCalendarDictionaries()
      .then((dictionaries) => {
        const map = new Map<string, string>();
        for (const group of [
          dictionaries.type,
          dictionaries.status,
          dictionaries.outcome,
        ]) {
          for (const entry of group) map.set(entry.value, entry.label);
        }
        setLabels(map);
      })
      .catch(() => undefined);
  }, []);

  const label = (value: string) => labels.get(value) ?? value;

  const now = useMemo(() => new Date(), []);
  const upcoming = events.filter((event) => new Date(event.endsAt) >= now);
  const past = events.filter((event) => new Date(event.endsAt) < now);

  if (loading) {
    return <p className="text-[13px] text-ink-muted">Wczytywanie terminów…</p>;
  }

  if (events.length === 0) {
    return (
      <p className="text-[13px] text-ink-muted">
        Brak terminów.{" "}
        <Link to="/kalendarz" className="text-accent hover:underline">
          Umów w kalendarzu
        </Link>
        .
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      {upcoming.length > 0 && (
        <Section title="Zaplanowane" events={upcoming} label={label} />
      )}
      {past.length > 0 && <Section title="Historia" events={past} label={label} />}
    </div>
  );
}

function Section({
  title,
  events,
  label,
}: {
  title: string;
  events: EventSummary[];
  label: (value: string) => string;
}) {
  return (
    <div>
      <h3 className="mb-1.5 text-[11px] font-medium tracking-wide text-ink-muted uppercase">
        {title}
      </h3>
      <ul className="divide-y divide-line rounded-md border border-line">
        {events.map((event) => {
          const Icon = eventIcon(event.type);
          const starts = new Date(event.startsAt);

          // items-center, nie items-start: data, ikona i odznaka to etykiety
          // całego wiersza, więc mają stać na jego środku. Przy wpisie
          // z rezultatem opis rośnie do trzech linijek i przyklejone do góry
          // wyglądały jak urwane.
          return (
            <li key={event.id} className="flex items-center gap-3 px-3 py-2">
              <span className="w-16 shrink-0 text-[12px] tabular-nums text-ink-secondary">
                {formatDayMonth(starts)}
                <span className="block text-ink-muted">
                  {event.allDay ? "cały dzień" : formatTime(event.startsAt)}
                </span>
              </span>

              <Icon
                className={cn("size-4 shrink-0", typeInk(event.type))}
                strokeWidth={2}
              />

              <span className="min-w-0 flex-1">
                <span
                  className={cn(
                    "block truncate text-[13px] text-ink",
                    event.status === "CANCELLED" && "text-ink-muted line-through",
                  )}
                >
                  {event.title}
                </span>
                <span className="block truncate text-[12px] text-ink-muted">
                  {[
                    label(event.type),
                    timeRange(event.startsAt, event.endsAt, event.allDay),
                    event.clientName ?? event.counterpartyName,
                    event.agentName,
                  ]
                    .filter(Boolean)
                    .join(" · ")}
                </span>
                {event.outcome && (
                  <span className="mt-0.5 block text-[12px] text-ink-secondary">
                    Rezultat: {label(event.outcome)}
                  </span>
                )}
              </span>

              <Badge tone={badgeTone[statusTone(event.status)]}>
                {label(event.status)}
              </Badge>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
