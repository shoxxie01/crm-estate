import { apiFetch } from "./client";

/**
 * Szybkie wyszukiwanie z nagłówka. Po kilka trafień z każdej grupy;
 * `…Total` mówi, ile jest wszystkich, żeby dało się zaproponować pełną listę.
 */

export interface ClientHit {
  id: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  email: string | null;
  status: string;
}

export interface PropertyHit {
  id: string;
  referenceNumber: string | null;
  title: string | null;
  propertyType: string;
  transactionType: string;
  status: string;
  city: string | null;
  district: string | null;
  street: string | null;
  price: number | null;
  priceCurrency: string | null;
}

export interface SearchResults {
  clients: ClientHit[];
  clientsTotal: number;
  properties: PropertyHit[];
  propertiesTotal: number;
}

/** Krótsze frazy backend i tak zbywa pustą odpowiedzią. */
export const SEARCH_MIN_LENGTH = 2;

export function quickSearch(query: string) {
  const params = new URLSearchParams({ q: query });
  return apiFetch<SearchResults>(`/search?${params}`);
}
