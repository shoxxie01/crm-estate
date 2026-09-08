// Kod pocztowy w formacie 00-000. Myślnik dopisuje się sam, więc agent wbija
// pięć cyfr i nie sięga po klawisz, którego na klawiaturze numerycznej telefonu
// zwykle nie ma pod ręką.
//
// W przeciwieństwie do telefonu (lib/phone.ts) formatujemy w trakcie pisania:
// wstawiany znak jest zawsze jeden i zawsze w tym samym miejscu, więc karetkę
// da się odtworzyć dokładnie — liczymy cyfry przed nią, a nie znaki.

/** Same cyfry, maks. 5, myślnik po drugiej. Bez efektów ubocznych — używalne też do testów. */
export function formatPostalCode(raw: string): string {
  const digits = raw.replace(/\D/g, "").slice(0, 5);
  if (digits.length <= 2) return digits;
  return `${digits.slice(0, 2)}-${digits.slice(2)}`;
}

/** Pozycja karetki tuż za `n`-tą cyfrą wartości. */
function caretAfterDigit(value: string, n: number): number {
  if (n <= 0) return 0;
  let seen = 0;
  for (let i = 0; i < value.length; i++) {
    if (value[i] >= "0" && value[i] <= "9" && ++seen === n) return i + 1;
  }
  return value.length;
}

/**
 * Maska pola: zwraca wartość do zapisania i pozycję karetki do przywrócenia.
 *
 * `raw` to wartość już zmieniona przez przeglądarkę, `caret` — pozycja po tej
 * zmianie, `previous` — wartość sprzed niej (potrzebna, by rozpoznać backspace
 * na samym myślniku).
 */
export function maskPostalCode(
  raw: string,
  caret: number,
  previous: string,
): { value: string; caret: number } {
  let digits = raw.replace(/\D/g, "").slice(0, 5);
  let digitsBefore = (raw.slice(0, caret).match(/\d/g) ?? []).length;

  // Backspace na myślniku skasowałby znak, który maska natychmiast dopisze
  // z powrotem — klawisz wyglądałby na zepsuty. Kasujemy więc cyfrę przed nim,
  // czyli to, co użytkownik faktycznie miał na myśli.
  if (
    raw.length < previous.length &&
    formatPostalCode(digits) === previous &&
    digitsBefore > 0
  ) {
    digits = digits.slice(0, digitsBefore - 1) + digits.slice(digitsBefore);
    digitsBefore -= 1;
  }

  // Myślnik wpisany ręcznie po dwóch cyfrach zostaje — inaczej znikałby pod
  // palcami i wracał dopiero przy trzeciej cyfrze. Tylko przy dopisywaniu:
  // po skasowaniu trzeciej cyfry ma zniknąć razem z nią, a nie zostać wiszący.
  const value =
    digits.length === 2 && raw.endsWith("-") && raw.length > previous.length
      ? `${digits}-`
      : formatPostalCode(digits);

  return { value, caret: caretAfterDigit(value, digitsBefore) };
}
