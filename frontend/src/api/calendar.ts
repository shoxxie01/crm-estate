import { apiFetch } from "./client";
import type { DictionaryEntry } from "./properties";

/**
 * Kontrakt modułu kalendarza.
 *
 * Wartości słownikowe (rodzaj, status, rezultat) są stringami — etykiety dają
 * `GET /api/calendar/dictionaries`, jak w pozostałych modułach.
 *
 * Czas leci jako ISO-8601 w UTC (`Instant` po stronie Javy). Front zamienia go
 * na czas lokalny dopiero przy wyświetlaniu — dzięki temu terminy nie skaczą
 * przy zmianie czasu na letni ani przy pracy z innej strefy.
 */

export interface CalendarDictionaries {
  type: DictionaryEntry[];
  status: DictionaryEntry[];
  outcome: DictionaryEntry[];
}

export interface EventSummary {
  id: string;
  type: string;
  status: string;
  outcome: string | null;
  title: string;
  startsAt: string;
  endsAt: string;
  allDay: boolean;
  location: string | null;
  agentId: string;
  agentName: string;
  propertyId: string | null;
  propertyReference: string | null;
  propertyTitle: string | null;
  clientId: string | null;
  clientName: string | null;
  counterpartyName: string | null;
  counterpartyPhone: string | null;
}

export interface EventDetail {
  summary: EventSummary;
  description: string | null;
  outcomeNote: string | null;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
  /** Inne terminy tego agenta zachodzące na ten zakres. Ostrzeżenie, nie błąd. */
  conflicts: EventSummary[];
}

export interface CreateEventPayload {
  type: string;
  status?: string;
  /** Pusty = serwer złoży tytuł z rodzaju i adresu oferty. */
  title?: string;
  description?: string;
  location?: string;
  startsAt: string;
  endsAt: string;
  allDay?: boolean;
  propertyId?: string | null;
  clientId?: string | null;
  counterpartyName?: string;
  counterpartyPhone?: string;
  agentId?: string;
  outcome?: string | null;
  outcomeNote?: string;
}

export interface EventFilters {
  agentId?: string;
  type?: string;
  status?: string;
  mine?: boolean;
}

export function fetchCalendarDictionaries() {
  return apiFetch<CalendarDictionaries>("/calendar/dictionaries");
}

/**
 * Terminy przecinające zakres. Zakres jest zamknięty i obowiązkowy — kalendarz
 * nigdy nie pyta „o wszystko", a serwer i tak odrzuci okno szersze niż pół roku.
 */
export function fetchEvents(from: Date, to: Date, filters: EventFilters = {}) {
  const query = new URLSearchParams({
    from: from.toISOString(),
    to: to.toISOString(),
  });
  if (filters.agentId) query.set("agentId", filters.agentId);
  if (filters.type) query.set("type", filters.type);
  if (filters.status) query.set("status", filters.status);
  if (filters.mine) query.set("mine", "true");

  return apiFetch<EventSummary[]>(`/calendar/events?${query}`);
}

export function fetchEvent(id: string) {
  return apiFetch<EventDetail>(`/calendar/events/${id}`);
}

export function createEvent(payload: CreateEventPayload) {
  return apiFetch<EventDetail>("/calendar/events", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateEvent(id: string, payload: CreateEventPayload) {
  return apiFetch<EventDetail>(`/calendar/events/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

/** Domknięcie terminu jednym ruchem — status, rezultat i notatka. */
export function updateEventStatus(
  id: string,
  payload: { status: string; outcome?: string | null; outcomeNote?: string },
) {
  return apiFetch<EventDetail>(`/calendar/events/${id}/status`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function deleteEvent(id: string) {
  return apiFetch<void>(`/calendar/events/${id}`, { method: "DELETE" });
}

/** Terminy powiązane z ofertą — sekcja „Terminy" na jej karcie. */
export function fetchPropertyEvents(propertyId: string) {
  return apiFetch<EventSummary[]>(`/properties/${propertyId}/events`);
}

/** Terminy powiązane z klientem. */
export function fetchClientEvents(clientId: string) {
  return apiFetch<EventSummary[]>(`/clients/${clientId}/events`);
}
