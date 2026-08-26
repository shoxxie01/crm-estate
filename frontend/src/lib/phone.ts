/**
 * Telefon po stronie formularzy.
 *
 * Kształt numeru narzuca serwer przy zapisie (PhoneNumber po stronie Javy),
 * więc tutaj tylko sprawdzamy, czy w polu jest coś, co numerem być może.
 * Pole celowo nie przeformatowuje się w trakcie pisania: kursor skacze wtedy
 * po wstawianiu spacji, a wklejony numer bywa cięty w połowie.
 */

/** Dziewięć do piętnastu cyfr, opcjonalny plus, spacje i myślniki dozwolone. */
const PHONE = /^\+?\d(?:[ -]?\d){8,14}$/;

export function isValidPhone(value: string): boolean {
  return PHONE.test(value.trim());
}
