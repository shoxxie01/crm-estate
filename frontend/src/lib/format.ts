const number = new Intl.NumberFormat("pl-PL");
const currency = new Intl.NumberFormat("pl-PL", {
  style: "currency",
  currency: "PLN",
  maximumFractionDigits: 0,
});

export const formatNumber = (value: number) => number.format(value);
export const formatCurrency = (value: number) => currency.format(value);

/** 4 250 000 -> "4,25 mln zł" — dla kafelków KPI, gdzie liczy się skanowalność. */
export function formatCompactPLN(value: number): string {
  if (Math.abs(value) >= 1_000_000) {
    return `${new Intl.NumberFormat("pl-PL", { maximumFractionDigits: 2 }).format(value / 1_000_000)} mln zł`;
  }
  if (Math.abs(value) >= 1_000) {
    return `${new Intl.NumberFormat("pl-PL", { maximumFractionDigits: 1 }).format(value / 1_000)} tys. zł`;
  }
  return formatCurrency(value);
}

export function formatDelta(value: number, unit: "pp" | "%" | "" = "%"): string {
  const sign = value > 0 ? "+" : value < 0 ? "−" : "";
  return `${sign}${number.format(Math.abs(value))}${unit}`;
}
