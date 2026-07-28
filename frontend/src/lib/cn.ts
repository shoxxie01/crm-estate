/** Skleja klasy CSS, pomijając wartości falsy (`cond && "klasa"`). */
export function cn(...parts: unknown[]): string {
  return parts.filter((part) => typeof part === "string" && part).join(" ");
}
