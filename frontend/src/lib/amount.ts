// Kwota w złotych bez groszy, grupowana spacjami w trakcie pisania: `600 000`.
// Przy sześciu, siedmiu zerach bez grupowania łatwo o pomyłkę rzędu wielkości,
// a w poszukiwaniu klienta grosze nie mają znaczenia.
//
// Karetkę odtwarzamy tak jak w kodzie pocztowym i telefonie. Po liczbie cyfr.

import { caretAfterDigit } from "./caret";

const MAX_DIGITS = 12;

export function formatAmount(raw: string): string {
  return raw
    .replace(/\D/g, "")
    .slice(0, MAX_DIGITS)
    .replace(/\B(?=(\d{3})+(?!\d))/g, " ");
}

/** Maska pola kwoty. Kontrakt jak `maskPhone`. */
export function maskAmount(
  raw: string,
  caret: number,
  previous: string,
  deleteForward = false,
): { value: string; caret: number } {
  let digits = raw.replace(/\D/g, "").slice(0, MAX_DIGITS);
  let digitsBefore = (raw.slice(0, caret).match(/\d/g) ?? []).length;

  // Skasowana sama spacja. Kasujemy cyfrę obok, zgodnie z kierunkiem klawisza.
  if (raw.length < previous.length && formatAmount(digits) === previous) {
    if (deleteForward) {
      digits = digits.slice(0, digitsBefore) + digits.slice(digitsBefore + 1);
    } else if (digitsBefore > 0) {
      digits = digits.slice(0, digitsBefore - 1) + digits.slice(digitsBefore);
      digitsBefore -= 1;
    }
  }

  const value = formatAmount(digits);
  return { value, caret: caretAfterDigit(value, digitsBefore) };
}

/** Wartość pola → liczba; `undefined` dla pustego. */
export function parseAmount(value: string): number | undefined {
  const digits = value.replace(/\D/g, "");
  return digits === "" ? undefined : Number(digits);
}
