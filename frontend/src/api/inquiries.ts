import { apiFetch } from "./client";
import type { DictionaryEntry } from "./properties";
import type { PropertyMatch } from "./matching";
import type { RequirementPayload } from "./clients";
import type { RequirementCriteria } from "../pages/clients/RequirementDetails";

/**
 * Publiczny formularz zgłoszeniowy i skrzynka zgłoszeń.
 *
 * Formularz (`/public/intake/{token}`) działa bez logowania. Biuro wskazuje
 * losowy klucz z linku. Zgłoszenie trafia do skrzynki, a nie od razu do klientów.
 */

export interface IntakeForm {
  agencyName: string;
  /** Treść zgód dokładnie w tej postaci, w jakiej serwer zapisze ją jako dowód. */
  consentProcessingText: string;
  consentMarketingText: string;
  privacyNote: string;
  propertyType: DictionaryEntry[];
  financing: DictionaryEntry[];
}

/** Formularz ma tylko te dwie intencje. Najem obsługuje agent. */
export type InquiryIntent = "BUY" | "SELL";

/** Nieruchomość, którą właściciel chce sprzedać przez biuro. */
export interface SaleOffer {
  propertyType: string;
  city: string;
  district?: string | null;
  area?: number | null;
  roomsCount?: number | null;
  expectedPrice?: number | null;
}

export interface PublicInquiryPayload {
  intent: InquiryIntent;
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  /** Tylko przy kupnie. */
  criteria?: RequirementPayload;
  /** Tylko przy sprzedaży. */
  offer?: SaleOffer;
  message?: string;
  consentProcessing: boolean;
  consentMarketing: boolean;
  /** Pole-pułapka na boty. Człowiek zostawia puste. */
  website?: string;
}

export interface PossibleDuplicate {
  id: string;
  name: string;
  phone: string | null;
  email: string | null;
  /** „telefon", „e-mail" */
  matchedBy: string[];
}

export interface Inquiry {
  id: string;
  status: "NEW" | "CONVERTED";
  intent: InquiryIntent;
  firstName: string;
  lastName: string;
  phone?: string | null;
  email?: string | null;
  /** Przy kupnie. */
  criteria?: RequirementCriteria | null;
  /** Przy sprzedaży. */
  offer?: SaleOffer | null;
  message?: string | null;
  consentProcessingAt: string;
  consentText: string;
  consentMarketing: boolean;
  possibleDuplicates: PossibleDuplicate[];
  clientId?: string | null;
  clientName?: string | null;
  handledByName?: string | null;
  handledAt?: string | null;
  createdAt: string;
}

export function fetchIntakeForm(token: string) {
  return apiFetch<IntakeForm>(`/public/intake/${encodeURIComponent(token)}`);
}

export function submitInquiry(token: string, payload: PublicInquiryPayload) {
  return apiFetch<void>(`/public/intake/${encodeURIComponent(token)}`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function fetchInquiries(status: "NEW" | "CONVERTED") {
  return apiFetch<Inquiry[]>(`/inquiries?status=${status}`);
}

export function fetchNewInquiryCount() {
  return apiFetch<{ new: number }>("/inquiries/count");
}

export function fetchInquiryMatches(id: string) {
  return apiFetch<PropertyMatch[]>(`/inquiries/${id}/matches`);
}

/** Bez `clientId` Nowy klient; z `clientId` dopięcie do istniejącego. */
export function convertInquiry(id: string, clientId?: string) {
  return apiFetch<{ clientId: string; requirementId: string }>(
    `/inquiries/${id}/convert`,
    { method: "POST", body: JSON.stringify({ clientId: clientId ?? null }) },
  );
}

export function rejectInquiry(id: string) {
  return apiFetch<void>(`/inquiries/${id}`, { method: "DELETE" });
}

export function fetchIntakeLink() {
  return apiFetch<{ token: string }>("/inquiries/intake-link");
}

export function regenerateIntakeLink() {
  return apiFetch<{ token: string }>("/inquiries/intake-link/regenerate", {
    method: "POST",
  });
}

/** Zdarzenie okna po zmianie w skrzynce. Menu boczne odświeża licznik. */
export const INQUIRIES_CHANGED = "delta:inquiries-changed";
