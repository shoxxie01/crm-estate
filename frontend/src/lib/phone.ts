/**
 * Telefon po stronie formularzy.
 *
 * CRM obsługuje polskie biuro, więc pole ma dwa tryby, wynikające z samej
 * wartości (bez osobnego przełącznika):
 * - krajowy (domyślny). Kierunkowy +48 stoi na stałe przy polu, agent wbija
 *   dziewięć cyfr, a spacje co trzy dopisuje maska w trakcie pisania,
 * - zagraniczny. Wartość zaczyna się od „+" (albo „00"); tu maski nie ma,
 *   bo grupowanie cyfr różni się między krajami.
 *
 * Na serwer zawsze idzie numer z kierunkowym. Ostateczny kształt i tak narzuca
 * PhoneNumber po stronie Javy.
 */

import { caretAfterDigit } from "./caret";

export const COUNTRY_PREFIX = "+48";

const NATIONAL_DIGITS = 9;

/** Kierunkowy i dziewięć do piętnastu cyfr, spacje i myślniki dozwolone. */
const INTERNATIONAL = /^\+\d(?:[ -]?\d){8,14}$/;

export const isInternational = (value: string): boolean =>
  value.trimStart().startsWith("+");

/** Maks. dziewięć cyfr pogrupowanych po trzy: `605 405 932`. */
export function formatNational(raw: string): string {
  return raw
    .replace(/\D/g, "")
    .slice(0, NATIONAL_DIGITS)
    .replace(/(\d{3})(?=\d)/g, "$1 ");
}

/** Cyfry numeru krajowego, jeśli `digits` to polski numer z kierunkowym. */
function stripPolishPrefix(digits: string): string | null {
  if (digits.length === 11 && digits.startsWith("48")) return digits.slice(2);
  if (digits.length === 13 && digits.startsWith("0048")) return digits.slice(4);
  return null;
}

/**
 * Maska pola. Odpowiednik `maskPostalCode`: zwraca wartość do zapisania
 * i pozycję karetki. `deleteForward` odróżnia klawisz Delete od Backspace,
 * gdy skasowana została sama spacja.
 */
export function maskPhone(
  raw: string,
  caret: number,
  previous: string,
  deleteForward = false,
): { value: string; caret: number } {
  const allDigits = raw.replace(/\D/g, "");

  // Wklejony (albo podpowiedziany przez przeglądarkę) polski numer
  // z kierunkowym wraca do trybu krajowego. +48 i tak stoi przy polu.
  const polish = stripPolishPrefix(allDigits);
  if (polish && (isInternational(raw) || allDigits.length > NATIONAL_DIGITS)) {
    const value = formatNational(polish);
    return { value, caret: value.length };
  }

  // Polski numer krajowy nie zaczyna się od zera, więc „00" to na pewno
  // prefiks wyjścia międzynarodowego. Zamieniamy go na plus.
  if (!isInternational(raw) && raw.trimStart().startsWith("00")) {
    const trimmed = raw.trimStart();
    const shift = raw.length - trimmed.length + 1;
    return { value: `+${trimmed.slice(2)}`, caret: Math.max(1, caret - shift) };
  }

  if (isInternational(raw)) return { value: raw, caret };

  let digits = allDigits;
  let digitsBefore = (raw.slice(0, caret).match(/\d/g) ?? []).length;

  // Numer jest już pełny. Cyfra dopisana w środku wypchnęłaby ostatnią,
  // więc ją odrzucamy i zostawiamy karetkę tam, gdzie była.
  const previousDigits = previous.replace(/\D/g, "").length;
  if (digits.length > NATIONAL_DIGITS && previousDigits === NATIONAL_DIGITS) {
    const extra = digits.length - NATIONAL_DIGITS;
    return {
      value: previous,
      caret: caretAfterDigit(previous, Math.max(0, digitsBefore - extra)),
    };
  }

  // Skasowana sama spacja wróciłaby natychmiast z maski i klawisz wyglądałby
  // na zepsuty. Kasujemy więc cyfrę obok niej, zgodnie z kierunkiem klawisza.
  if (raw.length < previous.length && formatNational(digits) === previous) {
    if (deleteForward) {
      digits = digits.slice(0, digitsBefore) + digits.slice(digitsBefore + 1);
    } else if (digitsBefore > 0) {
      digits = digits.slice(0, digitsBefore - 1) + digits.slice(digitsBefore);
      digitsBefore -= 1;
    }
  }

  // Spacja wpisana ręcznie po pełnej trójce zostaje. Inaczej znikałaby pod
  // palcami i wracała dopiero z następną cyfrą.
  const typedSeparator =
    raw.length > previous.length &&
    raw.endsWith(" ") &&
    (digits.length === 3 || digits.length === 6);
  const value = formatNational(digits) + (typedSeparator ? " " : "");

  return {
    value,
    caret: caretAfterDigit(value, Math.min(digitsBefore, NATIONAL_DIGITS)) +
      (typedSeparator && digitsBefore >= digits.length ? 1 : 0),
  };
}

/** Wartość z serwera → wartość pola. Polski numer traci kierunkowy, bo ten stoi przy polu. */
export function phoneToField(stored: string | null | undefined): string {
  if (!stored) return "";
  const digits = stored.replace(/\D/g, "");
  const polish = stripPolishPrefix(digits);
  if (polish) return formatNational(polish);
  if (!isInternational(stored) && digits.length === NATIONAL_DIGITS) {
    return formatNational(digits);
  }
  return stored;
}

/** Wartość pola → wartość do wysłania. Numer krajowy dostaje kierunkowy +48. */
export function phoneToPayload(field: string): string | undefined {
  const trimmed = field.trim();
  if (trimmed === "") return undefined;
  return isInternational(trimmed) ? trimmed : `${COUNTRY_PREFIX} ${trimmed}`;
}

/** Komunikat błędu albo `undefined`, gdy numer jest w porządku (lub pusty). */
export function phoneError(field: string): string | undefined {
  const trimmed = field.trim();
  if (trimmed === "") return undefined;
  if (isInternational(trimmed)) {
    return INTERNATIONAL.test(trimmed)
      ? undefined
      : "Podaj numer z kierunkowym kraju, np. +49 151 2345 6789.";
  }
  return trimmed.replace(/\D/g, "").length === NATIONAL_DIGITS
    ? undefined
    : "Numer musi mieć 9 cyfr, np. 605 405 932.";
}
