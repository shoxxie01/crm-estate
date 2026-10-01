/**
 * Dane demonstracyjne dashboardu. Docelowo: GET /api/dashboard/summary.
 * Kształt obiektów jest już taki, jaki powinien zwracać backend.
 */

export interface KpiDatum {
  id: string;
  label: string;
  value: number;
  format: "number" | "currencyCompact";
  delta: number;
  deltaUnit: "%" | "pp" | "";
  deltaLabel: string;
  higherIsBetter: boolean;
  trend: number[];
}

export const kpis: KpiDatum[] = [
  {
    id: "active-listings",
    label: "Aktywne oferty",
    value: 148,
    format: "number",
    delta: 8.2,
    deltaUnit: "%",
    deltaLabel: "vs poprzedni miesiąc",
    higherIsBetter: true,
    trend: [118, 121, 126, 124, 130, 133, 129, 136, 138, 141, 137, 148],
  },
  {
    id: "new-clients",
    label: "Nowi klienci",
    value: 63,
    format: "number",
    delta: 14.5,
    deltaUnit: "%",
    deltaLabel: "vs poprzednie 30 dni",
    higherIsBetter: true,
    trend: [38, 41, 44, 42, 47, 45, 51, 49, 55, 58, 55, 63],
  },
  {
    id: "signed-contracts",
    label: "Podpisane umowy",
    value: 11,
    format: "number",
    delta: -15.4,
    deltaUnit: "%",
    deltaLabel: "vs poprzedni miesiąc",
    higherIsBetter: true,
    trend: [9, 12, 10, 14, 13, 15, 12, 16, 14, 17, 13, 11],
  },
  {
    id: "transaction-value",
    label: "Wartość transakcji",
    value: 4_250_000,
    format: "currencyCompact",
    delta: 18.1,
    deltaUnit: "%",
    deltaLabel: "vs poprzedni miesiąc",
    higherIsBetter: true,
    trend: [
      2.1, 2.4, 2.2, 2.9, 3.1, 2.8, 3.4, 3.2, 3.8, 3.6, 3.9, 4.25,
    ].map((v) => v * 1_000_000),
  },
];

/** Transakcje domknięte w podziale na typ. 2 serie, wykres słupkowy skumulowany. */
export interface TransactionMonth {
  month: string;
  sale: number;
  rent: number;
}

export const transactionsByMonth: TransactionMonth[] = [
  { month: "sie", sale: 7, rent: 5 },
  { month: "wrz", sale: 9, rent: 6 },
  { month: "paź", sale: 8, rent: 8 },
  { month: "lis", sale: 11, rent: 7 },
  { month: "gru", sale: 6, rent: 9 },
  { month: "sty", sale: 5, rent: 11 },
  { month: "lut", sale: 8, rent: 8 },
  { month: "mar", sale: 12, rent: 9 },
  { month: "kwi", sale: 13, rent: 7 },
  { month: "maj", sale: 11, rent: 10 },
  { month: "cze", sale: 14, rent: 8 },
  { month: "lip", sale: 12, rent: 6 },
];

/** Źródła pozyskania klientów. Magnitude, więc kodowanie sekwencyjne. */
export interface LeadSource {
  label: string;
  value: number;
}

export const leadSources: LeadSource[] = [
  { label: "Otodom", value: 412 },
  { label: "Polecenia", value: 268 },
  { label: "Strona własna", value: 194 },
  { label: "OLX", value: 151 },
  { label: "Kampanie Meta", value: 88 },
];

/** Kalendarz. Najbliższe wydarzenia. */
export interface CalendarEvent {
  id: string;
  time: string;
  day: "Dziś" | "Jutro" | string;
  title: string;
  subtitle: string;
  kind: "Prezentacja" | "Spotkanie" | "Podpisanie" | "Wycena";
}

export const upcomingEvents: CalendarEvent[] = [
  {
    id: "e1",
    time: "10:30",
    day: "Dziś",
    title: "Prezentacja. Grzybowska 41",
    subtitle: "Marta Zielińska · 68 m², 2 pok.",
    kind: "Prezentacja",
  },
  {
    id: "e2",
    time: "13:00",
    day: "Dziś",
    title: "Podpisanie umowy pośrednictwa",
    subtitle: "Tomasz Wójcik · wyłączność 6 mies.",
    kind: "Podpisanie",
  },
  {
    id: "e3",
    time: "16:15",
    day: "Dziś",
    title: "Wycena lokalu usługowego",
    subtitle: "ul. Puławska 112 · 145 m²",
    kind: "Wycena",
  },
  {
    id: "e4",
    time: "09:00",
    day: "Jutro",
    title: "Spotkanie z inwestorem",
    subtitle: "Delta Invest sp. z o.o. · pakiet 4 lokali",
    kind: "Spotkanie",
  },
];

/** Status synchronizacji z portalami ogłoszeniowymi. */
export interface PortalSync {
  portal: string;
  status: "ok" | "warning" | "critical";
  listings: number;
  message: string;
}

export const portalSyncs: PortalSync[] = [
  {
    portal: "Otodom",
    status: "ok",
    listings: 148,
    message: "Zsynchronizowano 12 min temu",
  },
  {
    portal: "OLX",
    status: "ok",
    listings: 141,
    message: "Zsynchronizowano 12 min temu",
  },
  {
    portal: "Nieruchomosci-online",
    status: "warning",
    listings: 133,
    message: "15 ofert bez zdjęć. Pominięte",
  },
  {
    portal: "Gratka",
    status: "critical",
    listings: 0,
    message: "Błąd autoryzacji API. Odnów token",
  },
];

/** Klienci wymagający kontaktu. Dopasowanie do predyspozycji zakupowych. */
export interface HotLead {
  id: string;
  name: string;
  budget: string;
  looking: string;
  matches: number;
  lastContactDays: number;
}

export const hotLeads: HotLead[] = [
  {
    id: "c1",
    name: "Marta Zielińska",
    budget: "do 850 tys. zł",
    looking: "2 pok., Wola / Śródmieście",
    matches: 7,
    lastContactDays: 2,
  },
  {
    id: "c2",
    name: "Paweł Nowak",
    budget: "do 1,4 mln zł",
    looking: "dom, okolice Piaseczna",
    matches: 3,
    lastContactDays: 9,
  },
  {
    id: "c3",
    name: "Delta Invest sp. z o.o.",
    budget: "do 6 mln zł",
    looking: "lokale usługowe pod najem",
    matches: 5,
    lastContactDays: 1,
  },
  {
    id: "c4",
    name: "Katarzyna Lis",
    budget: "do 620 tys. zł",
    looking: "kawalerka, Mokotów",
    matches: 2,
    lastContactDays: 14,
  },
];
