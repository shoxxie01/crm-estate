import { formatCompactPLN, formatNumber } from "../../lib/format";

/**
 * Wspólne dla karty poszukiwania i jego formularza: etykiety z perspektywy
 * szukającego i formatowanie zakresów.
 *
 * Słownik TransactionType jest pisany z perspektywy oferty („Sprzedaż",
 * „Wynajem"), a poszukiwanie to druga strona tej samej transakcji.
 */
export const SEEKER_TRANSACTION: Record<string, string> = {
  SALE: "Kupno",
  RENT: "Najem",
};

/** Rodzaje, dla których liczba pokoi ma sens. */
export const ROOMS_TYPES = new Set(["APARTMENT", "HOUSE"]);

/** Rodzaje, dla których piętro ma sens. */
export const FLOOR_TYPES = new Set(["APARTMENT", "ROOM", "COMMERCIAL_UNIT"]);

/** Pusty wybór rodzaju = pytamy o wszystko, bo nie wiadomo jeszcze, czego nie. */
export const appliesTo = (allowed: Set<string>, selected: Iterable<string>) => {
  const list = [...selected];
  return list.length === 0 || list.some((type) => allowed.has(type));
};

function range(
  min: number | null,
  max: number | null,
  format: (value: number) => string,
): string | null {
  if (min != null && max != null) {
    return min === max ? format(min) : `${format(min)} – ${format(max)}`;
  }
  if (min != null) return `od ${format(min)}`;
  if (max != null) return `do ${format(max)}`;
  return null;
}

export const formatPriceRange = (min: number | null, max: number | null) =>
  range(min, max, formatCompactPLN);

export const formatAreaRange = (min: number | null, max: number | null) => {
  const text = range(min, max, formatNumber);
  return text && `${text} m²`;
};

export const formatCountRange = (min: number | null, max: number | null) =>
  range(min, max, formatNumber);

export const floorLabel = (floor: number) =>
  floor === -1 ? "suterena" : floor === 0 ? "parter" : formatNumber(floor);

export function formatFloorRange(
  min: number | null,
  max: number | null,
  excludeTop: boolean,
): string | null {
  const text = range(min, max, floorLabel);
  if (!excludeTop) return text;
  return text ? `${text}, bez ostatniego` : "bez ostatniego";
}

/** `2026-12-01` → `1.12.2026`, bez przesuwania strefą czasową. */
export function formatLocalDate(value: string): string {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day).toLocaleDateString("pl-PL");
}
