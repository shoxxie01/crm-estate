import { apiFetch } from "./client";
import type {
  DictionaryEntry,
  PageResponse,
  PropertySummary,
} from "./properties";

/**
 * Kontrakt modułu klientów.
 *
 * Wartości słownikowe (źródło, status) są stringami — listę etykiet daje
 * `GET /api/clients/dictionaries`, tak samo jak przy nieruchomościach.
 *
 * Rozróżnienie sprzedający/wynajmujący NIE jest polem klienta: przychodzi jako
 * `sellCount` / `rentCount` — liczba powierzonych ofert danego typu transakcji.
 */

export interface ClientDictionaries {
  source: DictionaryEntry[];
  status: DictionaryEntry[];
}

export interface ClientSummary {
  id: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  email: string | null;
  source: string | null;
  status: string;
  agentName: string;
  sellCount: number;
  rentCount: number;
  createdAt: string;
}

export interface ClientDetail {
  id: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  email: string | null;
  source: string | null;
  status: string;
  notes: string | null;
  agentId: string;
  agentName: string;
  sellCount: number;
  rentCount: number;
  ownedProperties: PropertySummary[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateClientPayload {
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  source?: string;
  status?: string;
  notes?: string;
}

export function fetchClientDictionaries() {
  return apiFetch<ClientDictionaries>("/clients/dictionaries");
}

export function fetchClients(
  params: { page?: number; status?: string; search?: string } = {},
) {
  const query = new URLSearchParams();
  if (params.page) query.set("page", String(params.page));
  if (params.status) query.set("status", params.status);
  if (params.search) query.set("search", params.search);

  const suffix = query.toString() ? `?${query}` : "";
  return apiFetch<PageResponse<ClientSummary>>(`/clients${suffix}`);
}

export function fetchClient(id: string) {
  return apiFetch<ClientDetail>(`/clients/${id}`);
}

export function createClient(payload: CreateClientPayload) {
  return apiFetch<ClientDetail>("/clients", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateClient(id: string, payload: CreateClientPayload) {
  return apiFetch<ClientDetail>(`/clients/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function deleteClient(id: string) {
  return apiFetch<void>(`/clients/${id}`, { method: "DELETE" });
}

/** Przypina istniejącą ofertę do klienta jako właściciela. */
export function assignProperty(clientId: string, propertyId: string) {
  return apiFetch<ClientDetail>(`/clients/${clientId}/properties/${propertyId}`, {
    method: "PUT",
  });
}

export function unassignProperty(clientId: string, propertyId: string) {
  return apiFetch<void>(`/clients/${clientId}/properties/${propertyId}`, {
    method: "DELETE",
  });
}
