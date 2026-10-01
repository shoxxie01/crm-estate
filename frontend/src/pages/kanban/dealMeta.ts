import type { DealCard } from "../../api/deals";
import { formatDayMonth, formatTime, sameDay, addDays } from "../calendar/dates";

const DAY = 24 * 60 * 60 * 1000;

/**
 * Po ilu dniach w jednym etapie karta zaczyna ostrzegać. Dwa tygodnie bez
 * ruchu w lejku to sygnał, że transakcja utknęła. Miesiąc, że umiera.
 */
export const STALE_AFTER_DAYS = 14;
export const DEAD_AFTER_DAYS = 30;

export function daysInStage(card: DealCard, now = Date.now()): number {
  return Math.max(0, Math.floor((now - new Date(card.stageChangedAt).getTime()) / DAY));
}

export function staleness(card: DealCard): "fresh" | "stale" | "dead" {
  if (card.closedAt) return "fresh";
  const days = daysInStage(card);
  if (days >= DEAD_AFTER_DAYS) return "dead";
  if (days >= STALE_AFTER_DAYS) return "stale";
  return "fresh";
}

export function daysLabel(days: number): string {
  if (days === 0) return "dziś";
  if (days === 1) return "1 dzień";
  return `${days} dni`;
}

/**
 * Rodzaj terminu, który zwykle jest następnym krokiem w danym etapie.
 * Podpowiedź w formularzu „Zaplanuj", nie reguła.
 */
const NEXT_STEP: Record<string, string> = {
  LEAD: "MEETING",
  VALUATION: "VALUATION",
  MANDATE: "PHOTO_SESSION",
  MARKETING: "PRESENTATION",
  NEGOTIATION: "MEETING",
  RESERVATION: "CONTRACT_SIGNING",
  CLOSING: "CONTRACT_SIGNING",
};

export function suggestedEventType(stage: string): string {
  return NEXT_STEP[stage] ?? "TASK";
}

/** „dziś 14:00", „jutro 9:30", „12 paź 10:00". Skrót na kartę. */
export function whenLabel(iso: string, allDay: boolean): string {
  const date = new Date(iso);
  const today = new Date();
  const day = sameDay(date, today)
    ? "dziś"
    : sameDay(date, addDays(today, 1))
      ? "jutro"
      : formatDayMonth(date);
  return allDay ? day : `${day} ${formatTime(iso)}`;
}

/** Inicjały na awatar agenta. */
export function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]!.toUpperCase())
    .join("");
}

// --- terminy umowne ----------------------------------------------------------

/** Od ilu dni przed terminem karta ostrzega na żółto, a od ilu na czerwono. */
export const DEADLINE_WARN_DAYS = 14;
export const DEADLINE_CRITICAL_DAYS = 3;

/**
 * Ile dni zostało do terminu (ujemne = po terminie). Liczone na datach
 * lokalnych, nie na milisekundach. „Jutro" ma być jutro także o 23:59.
 */
export function daysUntil(dueDate: string, today = new Date()): number {
  const [year, month, day] = dueDate.split("-").map(Number);
  const due = Date.UTC(year!, month! - 1, day!);
  const now = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
  return Math.round((due - now) / DAY);
}

export function deadlineTone(days: number): "neutral" | "warning" | "critical" {
  if (days <= DEADLINE_CRITICAL_DAYS) return "critical";
  if (days <= DEADLINE_WARN_DAYS) return "warning";
  return "neutral";
}

/** „dziś", „jutro", „za 9 dni", „2 dni po terminie". */
export function deadlineWhen(days: number): string {
  if (days === 0) return "dziś";
  if (days === 1) return "jutro";
  if (days > 1) return `za ${days} dni`;
  return days === -1 ? "1 dzień po terminie" : `${-days} dni po terminie`;
}

/** Data `2026-12-31` po polsku: „31 gru 2026". */
export function formatDueDate(dueDate: string): string {
  const [year, month, day] = dueDate.split("-").map(Number);
  return new Date(year!, month! - 1, day!).toLocaleDateString("pl-PL", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

/** Rodzaj terminu, który zwykle dochodzi na danym etapie. Podpowiedź w formularzu. */
const DEADLINE_FOR_STAGE: Record<string, string> = {
  LEAD: "MANDATE_END",
  VALUATION: "MANDATE_END",
  MANDATE: "MANDATE_END",
  MARKETING: "MANDATE_END",
  NEGOTIATION: "RESERVATION_END",
  RESERVATION: "FINAL_CONTRACT",
  CLOSING: "NOTARY_DEED",
};

export function suggestedDeadlineType(stage: string): string {
  return DEADLINE_FOR_STAGE[stage] ?? "OTHER";
}

/** Terminy, przed którymi warto zaplanować rozmowę z właścicielem o przedłużeniu. */
export const RENEWABLE_DEADLINES = ["MANDATE_END", "EXCLUSIVITY_END"];
