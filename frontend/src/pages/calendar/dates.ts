/**
 * Pomocniki dat kalendarza.
 *
 * Wszystko liczymy w czasie lokalnym przeglądarki. Z API przychodzą punkty
 * w czasie (UTC), a siatka dni jest z natury lokalna: „poniedziałek" zaczyna się
 * o północy u agenta, nie w Greenwich. Tydzień zaczyna się w poniedziałek, jak
 * w polskich kalendarzach.
 */

export type CalendarView = "month" | "week" | "day";

export function startOfDay(date: Date): Date {
  const copy = new Date(date);
  copy.setHours(0, 0, 0, 0);
  return copy;
}

export function addDays(date: Date, days: number): Date {
  const copy = new Date(date);
  copy.setDate(copy.getDate() + days);
  return copy;
}

export function addMonths(date: Date, months: number): Date {
  const copy = new Date(date);
  // Dzień ustawiamy na 1 przed przesunięciem: inaczej 31 marca minus miesiąc
  // daje 3 marca, bo luty nie ma 31 dni.
  copy.setDate(1);
  copy.setMonth(copy.getMonth() + months);
  return copy;
}

/** Poniedziałek tygodnia, w którym leży data. */
export function startOfWeek(date: Date): Date {
  const copy = startOfDay(date);
  const weekday = (copy.getDay() + 6) % 7; // niedziela (0) -> 6
  return addDays(copy, -weekday);
}

export function startOfMonth(date: Date): Date {
  const copy = startOfDay(date);
  copy.setDate(1);
  return copy;
}

/** Pierwszy dzień siatki miesiąca. Poniedziałek poprzedzający pierwszy dzień. */
export function startOfMonthGrid(date: Date): Date {
  return startOfWeek(startOfMonth(date));
}

export function sameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

/** Zakres [od, do) pokrywający widok. Dokładnie o to pytamy backend. */
export function rangeFor(view: CalendarView, anchor: Date): [Date, Date] {
  if (view === "day") {
    const from = startOfDay(anchor);
    return [from, addDays(from, 1)];
  }
  if (view === "week") {
    const from = startOfWeek(anchor);
    return [from, addDays(from, 7)];
  }
  const from = startOfMonthGrid(anchor);
  return [from, addDays(from, 42)];
}

export function shift(view: CalendarView, anchor: Date, direction: 1 | -1): Date {
  if (view === "day") return addDays(anchor, direction);
  if (view === "week") return addDays(anchor, 7 * direction);
  return addMonths(anchor, direction);
}

const time = new Intl.DateTimeFormat("pl-PL", {
  hour: "2-digit",
  minute: "2-digit",
});
const dayLong = new Intl.DateTimeFormat("pl-PL", {
  weekday: "long",
  day: "numeric",
  month: "long",
});
const dayShort = new Intl.DateTimeFormat("pl-PL", { weekday: "short" });
const monthYear = new Intl.DateTimeFormat("pl-PL", {
  month: "long",
  year: "numeric",
});
const dayMonth = new Intl.DateTimeFormat("pl-PL", {
  day: "numeric",
  month: "short",
});

export const formatTime = (iso: string) => time.format(new Date(iso));
export const formatDayLong = (date: Date) => dayLong.format(date);
export const formatDayShort = (date: Date) => dayShort.format(date);
export const formatDayMonth = (date: Date) => dayMonth.format(date);

/** Podpis nad siatką: „sierpień 2026" albo „17–23 sie 2026" dla tygodnia. */
export function periodLabel(view: CalendarView, anchor: Date): string {
  if (view === "day") return formatDayLong(anchor);
  if (view === "month") return monthYear.format(anchor);

  const from = startOfWeek(anchor);
  return `${formatDayMonth(from)} – ${formatDayMonth(addDays(from, 6))} ${from.getFullYear()}`;
}

/** Godziny terminu: „10:30–11:30" albo „cały dzień". */
export function timeRange(startsAt: string, endsAt: string, allDay: boolean): string {
  return allDay ? "cały dzień" : `${formatTime(startsAt)}–${formatTime(endsAt)}`;
}

// --- pola formularza -------------------------------------------------------

/** Data w formacie `<input type="date">`. Lokalna, nie UTC. */
export function toDateInput(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

/** Godzina w formacie `<input type="time">`. */
export function toTimeInput(date: Date): string {
  const hours = `${date.getHours()}`.padStart(2, "0");
  const minutes = `${date.getMinutes()}`.padStart(2, "0");
  return `${hours}:${minutes}`;
}

/**
 * Składa lokalną datę i godzinę w punkt w czasie. Rozbicie na dwa pola jest
 * świadome: `datetime-local` bywa nieprzewidywalny na telefonach, a agent i tak
 * najczęściej zmienia samą godzinę.
 */
export function fromInputs(dateValue: string, timeValue: string): Date | null {
  if (!dateValue) return null;
  const [year, month, day] = dateValue.split("-").map(Number);
  const [hours, minutes] = (timeValue || "00:00").split(":").map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day, hours || 0, minutes || 0, 0, 0);
}
