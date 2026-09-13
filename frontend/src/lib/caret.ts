// Wspólne dla masek pól (kod pocztowy, telefon): maska wstawia własne
// separatory, więc karetkę odtwarzamy po liczbie cyfr przed nią, a nie znaków.

/** Pozycja karetki tuż za `n`-tą cyfrą wartości. */
export function caretAfterDigit(value: string, n: number): number {
  if (n <= 0) return 0;
  let seen = 0;
  for (let i = 0; i < value.length; i++) {
    if (value[i] >= "0" && value[i] <= "9" && ++seen === n) return i + 1;
  }
  return value.length;
}
