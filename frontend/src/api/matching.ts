import { apiFetch } from "./client";
import type { ClientRequirement } from "./clients";
import type { PropertySummary } from "./properties";

/**
 * Dopasowanie ofert do poszukiwań. Liczone na serwerze na żądanie. W odpowiedzi
 * są wyłącznie pasujące pozycje, każda z wyjaśnieniem kryterium po kryterium.
 */

/** MET spełnione, NEAR prawie (np. cena do 10% ponad budżet), UNKNOWN brak danych w ofercie. */
export type MatchVerdict = "MET" | "NEAR" | "MISSED" | "UNKNOWN";

export interface MatchCriterion {
  criterion: string;
  label: string;
  verdict: MatchVerdict;
  note: string | null;
}

export interface ClientMatch {
  clientId: string;
  clientName: string;
  phone: string | null;
  email: string | null;
  agentName: string;
  requirement: ClientRequirement;
  criteria: MatchCriterion[];
  /** Kryteria „prawie" i „brak danych". Do dopytania. 0 = pewne dopasowanie. */
  warnings: number;
}

export interface PropertyMatch {
  property: PropertySummary;
  criteria: MatchCriterion[];
  warnings: number;
}

export function fetchPropertyMatches(propertyId: string) {
  return apiFetch<ClientMatch[]>(`/properties/${propertyId}/matches`);
}

export function fetchRequirementMatches(clientId: string, requirementId: string) {
  return apiFetch<PropertyMatch[]>(
    `/clients/${clientId}/requirements/${requirementId}/matches`,
  );
}
