import { apiFetch } from "./client";
import type { EventSummary } from "./calendar";
import type { DictionaryEntry } from "./properties";

/**
 * Kontrakt tablicy Kanban transakcji.
 *
 * Etapy przychodzą ze słownika w kolejności kolumn. Front nie trzyma własnej
 * listy, więc nowy etap dopisany w backendzie pojawi się na tablicy sam.
 */

export interface DealDictionaries {
  stage: DictionaryEntry[];
  lostReason: DictionaryEntry[];
  interestStatus: DictionaryEntry[];
  deadlineType: DictionaryEntry[];
  deadlineStatus: DictionaryEntry[];
}

export interface DealCard {
  id: string;
  title: string;
  stage: string;
  stageChangedAt: string;
  closedAt: string | null;
  value: number | null;
  commission: number | null;
  agentId: string;
  agentName: string;
  clientId: string | null;
  clientName: string | null;
  /** Kupujący z bazy. Ustawia go przyjęta oferta na liście zainteresowanych. */
  buyerId: string | null;
  /** Nazwa kupującego, także spoza bazy (z przyjętej oferty). */
  buyerName: string | null;
  propertyId: string | null;
  propertyReference: string | null;
  propertyAddress: string | null;
  transactionType: string | null;
  lostReason: string | null;
  /** Najbliższy aktywny termin. Pusty na otwartej karcie = brak zaplanowanego kroku. */
  nextEvent: EventSummary | null;
  /** Zainteresowani wciąż w grze (bez tych, którzy odpadli). */
  interestCount: number;
  offerCount: number;
  bestOffer: number | null;
  /** Najbliższy otwarty termin umowny. */
  nextDeadline?: DeadlineEntry | null;
  createdAt: string;
}

/**
 * Termin umowny. Data z umowy, której biuro pilnuje. `dueDate` to data
 * kalendarzowa (`2026-12-31`), nie znacznik czasu: umowa nie mówi o godzinie.
 */
export interface DeadlineEntry {
  id: string;
  type: string;
  dueDate: string;
  status: string;
  note?: string | null;
  /** Poprzednia data, gdy termin powstał przez przesunięcie (aneks). */
  movedFromDate?: string | null;
  resolvedAt?: string | null;
}

/** Termin umowny w kalendarzu. Tylko do odczytu, prowadzi do karty. */
export interface CalendarDeadline {
  id: string;
  type: string;
  dueDate: string;
  status: string;
  note?: string | null;
  dealId: string;
  dealTitle: string;
  agentId: string;
  agentName: string;
}

/** Jedna osoba zainteresowana ofertą. Klient z bazy albo imię i telefon. */
export interface InterestEntry {
  id: string;
  clientId: string | null;
  name: string;
  phone: string | null;
  status: string;
  offerAmount: number | null;
  note: string | null;
  createdAt: string;
}

export interface InterestPayload {
  clientId?: string | null;
  name?: string;
  phone?: string;
  status?: string;
  offerAmount?: number | null;
  note?: string;
}

export interface StageChangeEntry {
  fromStage: string | null;
  toStage: string;
  changedByName: string;
  changedAt: string;
}

export interface DealDetail {
  card: DealCard;
  notes: string | null;
  lostNote: string | null;
  createdByName: string;
  updatedAt: string;
  history: StageChangeEntry[];
  events: EventSummary[];
  interests: InterestEntry[];
  deadlines: DeadlineEntry[];
}

export interface DealPayload {
  title?: string;
  /** Brany pod uwagę tylko przy dodawaniu. */
  stage?: string;
  agentId?: string;
  clientId?: string | null;
  propertyId?: string | null;
  value?: number | null;
  commission?: number | null;
  notes?: string;
}

export const CLOSED_STAGES = ["WON", "LOST"];

export function fetchDealDictionaries() {
  return apiFetch<DealDictionaries>("/deals/dictionaries");
}

export function fetchBoard(filters: { mine?: boolean } = {}) {
  const query = filters.mine ? "?mine=true" : "";
  return apiFetch<DealCard[]>(`/deals${query}`);
}

export function fetchDeal(id: string) {
  return apiFetch<DealDetail>(`/deals/${id}`);
}

export function createDeal(payload: DealPayload) {
  return apiFetch<DealDetail>("/deals", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateDeal(id: string, payload: DealPayload) {
  return apiFetch<DealDetail>(`/deals/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function changeDealStage(
  id: string,
  payload: { stage: string; lostReason?: string | null; lostNote?: string },
) {
  return apiFetch<DealDetail>(`/deals/${id}/stage`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function deleteDeal(id: string) {
  return apiFetch<void>(`/deals/${id}`, { method: "DELETE" });
}

export function addInterest(dealId: string, payload: InterestPayload) {
  return apiFetch<DealDetail>(`/deals/${dealId}/interests`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateInterest(dealId: string, interestId: string, payload: InterestPayload) {
  return apiFetch<DealDetail>(`/deals/${dealId}/interests/${interestId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function deleteInterest(dealId: string, interestId: string) {
  return apiFetch<DealDetail>(`/deals/${dealId}/interests/${interestId}`, {
    method: "DELETE",
  });
}

// --- raport lejka ------------------------------------------------------------

/**
 * Raport lejka. Okres wyznacza kohortę transakcji założonych w tym czasie;
 * prognoza dotyczy kart otwartych dziś. Puste wartości liczbowe znaczą
 * „za mało danych", a nie zero.
 */
export interface DealReport {
  from: string;
  to: string;
  totals: {
    created: number;
    open: number;
    won: number;
    lost: number;
    winRate?: number | null;
    wonValue: number;
    wonCommission: number;
    avgCycleDays?: number | null;
  };
  funnel: {
    stage: string;
    reached: number;
    conversion?: number | null;
    lostHere: number;
  }[];
  stageTimes: {
    stage: string;
    samples: number;
    medianDays?: number | null;
    avgDays?: number | null;
  }[];
  lostReasons: { reason: string; count: number }[];
  agents: {
    agentId: string;
    agentName: string;
    created: number;
    won: number;
    lost: number;
    winRate?: number | null;
    wonValue: number;
  }[];
  forecast: {
    openValue: number;
    expectedValue: number;
    stages: {
      stage: string;
      openCount: number;
      openValue: number;
      winRate?: number | null;
      sample: number;
      expectedValue?: number | null;
    }[];
  };
}

export function fetchDealReport(params: { from?: Date; to?: Date; mine?: boolean }) {
  const query = new URLSearchParams();
  if (params.from) query.set("from", params.from.toISOString());
  if (params.to) query.set("to", params.to.toISOString());
  if (params.mine) query.set("mine", "true");
  return apiFetch<DealReport>(`/deals/report?${query}`);
}

// --- terminy umowne ----------------------------------------------------------

export function addDeadline(dealId: string, payload: { type: string; dueDate: string; note?: string }) {
  return apiFetch<DealDetail>(`/deals/${dealId}/deadlines`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function markDeadlineMet(dealId: string, deadlineId: string) {
  return apiFetch<DealDetail>(`/deals/${dealId}/deadlines/${deadlineId}/met`, { method: "PUT" });
}

export function reopenDeadline(dealId: string, deadlineId: string) {
  return apiFetch<DealDetail>(`/deals/${dealId}/deadlines/${deadlineId}/reopen`, { method: "PUT" });
}

/** Przesunięcie (aneks). Stara data zostaje w historii. */
export function moveDeadline(dealId: string, deadlineId: string, payload: { dueDate: string; note?: string }) {
  return apiFetch<DealDetail>(`/deals/${dealId}/deadlines/${deadlineId}/move`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function deleteDeadline(dealId: string, deadlineId: string) {
  return apiFetch<DealDetail>(`/deals/${dealId}/deadlines/${deadlineId}`, { method: "DELETE" });
}

/** Warstwa terminów umownych w kalendarzu. Daty lokalne, `to` wyłączne. */
export function fetchCalendarDeadlines(from: string, to: string, mine = false) {
  const query = new URLSearchParams({ from, to });
  if (mine) query.set("mine", "true");
  return apiFetch<CalendarDeadline[]>(`/calendar/deadlines?${query}`);
}
