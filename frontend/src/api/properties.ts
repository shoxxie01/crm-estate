import { apiFetch } from "./client";

/**
 * Kontrakt modułu nieruchomości.
 *
 * Wartości słownikowe są tu zwykłymi stringami, a nie uniami literałów —
 * listy pochodzą z `GET /api/properties/dictionaries`, więc powielanie
 * dwudziestu kilku enumów w TypeScripcie oznaczałoby dwa źródła prawdy
 * rozjeżdżające się przy każdej zmianie po stronie backendu.
 */

export interface DictionaryEntry {
  value: string;
  label: string;
}

export interface FeatureGroup {
  category: string;
  label: string;
  features: DictionaryEntry[];
}

export interface Dictionaries {
  propertyType: DictionaryEntry[];
  transactionType: DictionaryEntry[];
  marketType: DictionaryEntry[];
  status: DictionaryEntry[];
  currency: DictionaryEntry[];
  voivodeship: DictionaryEntry[];
  ownershipForm: DictionaryEntry[];
  buildingType: DictionaryEntry[];
  buildingMaterial: DictionaryEntry[];
  constructionStatus: DictionaryEntry[];
  windowsType: DictionaryEntry[];
  roofType: DictionaryEntry[];
  roofing: DictionaryEntry[];
  garretType: DictionaryEntry[];
  surroundings: DictionaryEntry[];
  heatingType: DictionaryEntry[];
  plotType: DictionaryEntry[];
  roadAccess: DictionaryEntry[];
  commercialUse: DictionaryEntry[];
  hallStructure: DictionaryEntry[];
  flooring: DictionaryEntry[];
  parkingType: DictionaryEntry[];
  energyClass: DictionaryEntry[];
  featureGroups: FeatureGroup[];
}

export interface PropertySummary {
  id: string;
  referenceNumber: string;
  title: string;
  propertyType: string;
  transactionType: string;
  marketType: string;
  status: string;
  price: number;
  priceCurrency: string;
  pricePerSquareMeter: number | null;
  totalArea: number;
  roomsCount: number | null;
  city: string;
  district: string | null;
  agentName: string;
  readyForExport: boolean;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/** Numeru oferty tu nie ma — nadaje go backend przy zapisie. */
export interface CreatePropertyPayload {
  propertyType: string;
  transactionType: string;
  marketType: string;
  status?: string;
  title: string;
  description: string;
  pricing: {
    price: number;
    priceCurrency?: string;
    priceNegotiable?: boolean;
    rent?: number | null;
    rentCurrency?: string;
    priceIncludesRent?: boolean;
    deposit?: number | null;
    depositCurrency?: string;
    commissionPercent?: number | null;
  };
  area: {
    totalArea: number;
    usableArea?: number | null;
    plotArea?: number | null;
    roomsCount?: number | null;
    bathroomsCount?: number | null;
    floorNo?: number | null;
    buildingFloorsCount?: number | null;
    ceilingHeight?: number | null;
  };
  address: {
    voivodeship: string;
    county?: string;
    commune?: string;
    city: string;
    district?: string;
    street?: string;
    buildingNumber?: string;
    apartmentNumber?: string;
    postalCode?: string;
    latitude?: number | null;
    longitude?: number | null;
    hideExactAddress?: boolean;
  };
  building?: Record<string, unknown>;
  energy?: Record<string, unknown>;
  land?: Record<string, unknown>;
  commercial?: Record<string, unknown>;
  availableFrom?: string | null;
  features?: string[];
  heatingTypes?: string[];
  commercialUses?: string[];
  videoUrl?: string;
  panoramaUrl?: string;
  privateNotes?: string;
  keysInfo?: string;
  exportable?: boolean;
}

/** Pełny widok oferty — odpowiada PropertyResponse z backendu. */
export interface PropertyDetail {
  id: string;
  referenceNumber: string;
  propertyType: string;
  transactionType: string;
  marketType: string;
  status: string;
  title: string;
  description: string;
  pricing: {
    price: number;
    priceCurrency: string;
    priceNegotiable: boolean;
    pricePerSquareMeter: number | null;
    rent: number | null;
    rentCurrency: string | null;
    priceIncludesRent: boolean;
    deposit: number | null;
    depositCurrency: string | null;
    commissionPercent: number | null;
  };
  area: {
    totalArea: number;
    usableArea: number | null;
    plotArea: number | null;
    roomsCount: number | null;
    bathroomsCount: number | null;
    floorNo: number | null;
    buildingFloorsCount: number | null;
    ceilingHeight: number | null;
  };
  address: {
    countryCode: string;
    voivodeship: string;
    county: string | null;
    commune: string | null;
    city: string;
    district: string | null;
    street: string | null;
    buildingNumber: string | null;
    apartmentNumber: string | null;
    postalCode: string | null;
    latitude: number | null;
    longitude: number | null;
    hideExactAddress: boolean;
  };
  building: {
    buildYear: number | null;
    buildingType: string | null;
    buildingMaterial: string | null;
    constructionStatus: string | null;
    windowsType: string | null;
    ownershipForm: string | null;
    surroundings: string | null;
    furnished: boolean | null;
  };
  energy: {
    energyPrimary: number | null;
    energyFinal: number | null;
    energyClass: string | null;
    certificateNumber: string | null;
    exempt: boolean;
    exemptNote: string | null;
  };
  land: {
    plotType: string | null;
    dimensions: string | null;
    roadAccess: string | null;
    fenced: boolean | null;
    zoningPlan: string | null;
  };
  commercial: {
    structure: string | null;
    flooring: string | null;
    parkingType: string | null;
    officeSpace: boolean | null;
    socialFacilities: boolean | null;
    loadingRamp: boolean | null;
  };
  availableFrom: string | null;
  features: string[];
  heatingTypes: string[];
  commercialUses: string[];
  videoUrl: string | null;
  panoramaUrl: string | null;
  privateNotes: string | null;
  keysInfo: string | null;
  exportable: boolean;
}

export function fetchDictionaries() {
  return apiFetch<Dictionaries>("/properties/dictionaries");
}

export function fetchProperties(params: {
  page?: number;
  status?: string;
  type?: string;
  transaction?: string;
} = {}) {
  const query = new URLSearchParams();
  if (params.page) query.set("page", String(params.page));
  if (params.status) query.set("status", params.status);
  if (params.type) query.set("type", params.type);
  if (params.transaction) query.set("transaction", params.transaction);

  const suffix = query.toString() ? `?${query}` : "";
  return apiFetch<PageResponse<PropertySummary>>(`/properties${suffix}`);
}

export function fetchProperty(id: string) {
  return apiFetch<PropertyDetail>(`/properties/${id}`);
}

export function createProperty(payload: CreatePropertyPayload) {
  return apiFetch<PropertyDetail>("/properties", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateProperty(id: string, payload: CreatePropertyPayload) {
  return apiFetch<PropertyDetail>(`/properties/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function deleteProperty(id: string) {
  return apiFetch<void>(`/properties/${id}`, { method: "DELETE" });
}
