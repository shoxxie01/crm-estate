import { apiFetch } from "./client";
import type {
  DictionaryEntry,
  PageResponse,
  PropertySummary,
} from "./properties";

/**
 * Kontrakt modułu klientów.
 *
 * Wartości słownikowe (źródło, status) są stringami. Listę etykiet daje
 * `GET /api/clients/dictionaries`, tak samo jak przy nieruchomościach.
 *
 * Role NIE są polami klienta: `sellCount` / `rentCount` to liczba powierzonych
 * ofert danego typu transakcji, a `buyerCount` / `tenantCount`. Liczba
 * aktywnych poszukiwań kupna i najmu.
 */

export interface ClientDictionaries {
  source: DictionaryEntry[];
  status: DictionaryEntry[];
  requirementStatus: DictionaryEntry[];
  financing: DictionaryEntry[];
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
  buyerCount: number;
  tenantCount: number;
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
  buyerCount: number;
  tenantCount: number;
  ownedProperties: PropertySummary[];
  /** Aktywne najpierw, potem historia. */
  requirements: ClientRequirement[];
  createdAt: string;
  updatedAt: string;
}

/* --- poszukiwania -------------------------------------------------------- */

export interface RequirementLocation {
  city: string;
  district: string | null;
}

/** Czego klient szuka. Każda granica zakresu z osobna może być pusta. */
export interface ClientRequirement {
  id: string;
  clientId: string;
  status: string;
  /** SALE = klient kupuje, RENT = klient najmuje. */
  transactionType: string;
  propertyTypes: string[];
  marketType: string | null;
  locations: RequirementLocation[];
  priceMin: number | null;
  priceMax: number | null;
  areaMin: number | null;
  areaMax: number | null;
  roomsMin: number | null;
  roomsMax: number | null;
  floorMin: number | null;
  floorMax: number | null;
  excludeTopFloor: boolean;
  financing: string | null;
  moveInDate: string | null;
  requiredFeatures: string[];
  preferredFeatures: string[];
  notes: string | null;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

export interface RequirementPayload {
  transactionType: string;
  status?: string;
  propertyTypes: string[];
  marketType?: string;
  locations: { city: string; district?: string }[];
  priceMin?: number;
  priceMax?: number;
  areaMin?: number;
  areaMax?: number;
  roomsMin?: number;
  roomsMax?: number;
  floorMin?: number;
  floorMax?: number;
  excludeTopFloor: boolean;
  financing?: string;
  moveInDate?: string;
  requiredFeatures: string[];
  preferredFeatures: string[];
  notes?: string;
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

export function createRequirement(clientId: string, payload: RequirementPayload) {
  return apiFetch<ClientRequirement>(`/clients/${clientId}/requirements`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateRequirement(
  clientId: string,
  id: string,
  payload: RequirementPayload,
) {
  return apiFetch<ClientRequirement>(`/clients/${clientId}/requirements/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function changeRequirementStatus(clientId: string, id: string, status: string) {
  return apiFetch<ClientRequirement>(
    `/clients/${clientId}/requirements/${id}/status`,
    { method: "PUT", body: JSON.stringify({ status }) },
  );
}

export function deleteRequirement(clientId: string, id: string) {
  return apiFetch<void>(`/clients/${clientId}/requirements/${id}`, {
    method: "DELETE",
  });
}
