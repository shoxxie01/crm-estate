import { apiFetch } from "./client";

/**
 * Punkt na mapie razem z rozłożonym adresem. Pola adresowe nazywają się tak
 * samo jak w formularzu oferty, więc przepisuje się je jeden do jednego.
 *
 * Backend pomija w JSON-ie pola puste (`default-property-inclusion: non_null`),
 * stąd wszystkie są opcjonalne. Brak pola znaczy „geokoder tego nie podał".
 */
export interface GeoLocation {
  latitude: number;
  longitude: number;
  displayName?: string;
  voivodeship?: string;
  county?: string;
  commune?: string;
  city?: string;
  district?: string;
  street?: string;
  buildingNumber?: string;
  postalCode?: string;
}

/** Adres pod pinezką. `null`, gdy w tym punkcie nie ma czego opisać (las, woda). */
export async function reverseGeocode(
  latitude: number,
  longitude: number,
): Promise<GeoLocation | null> {
  const query = new URLSearchParams({
    lat: latitude.toFixed(6),
    lon: longitude.toFixed(6),
  });
  // 204 wraca z apiFetch jako undefined. Na zewnątrz ujednolicamy do null.
  const location = await apiFetch<GeoLocation | undefined>(
    `/geo/reverse?${query}`,
  );
  return location ?? null;
}

/** Pola adresu, po których szukamy punktu. Wymagane: województwo i miejscowość. */
export interface GeoQuery {
  voivodeship: string;
  city: string;
  district?: string;
  street?: string;
  buildingNumber?: string;
  postalCode?: string;
}

/**
 * Czy z tego, co jest w formularzu, da się w ogóle postawić pinezkę.
 *
 * Sama miejscowość nie wystarcza: nazwy się powtarzają (samych „Nowych Wsi"
 * jest w Polsce ponad sto), więc bez województwa pinezka lądowałaby losowo.
 * Reszta pól tylko zawęża wynik. Ulica schodzi z centrum miejscowości na
 * ulicę, numer budynku na budynek, a kod pocztowy rozdziela miejscowości
 * o tej samej nazwie w jednym województwie.
 */
export function canGeocode(query: Partial<GeoQuery>): query is GeoQuery {
  return Boolean(query.voivodeship && query.city?.trim());
}

/** Punkty pasujące do adresu, od najlepiej dopasowanego. Pusta lista = nie znaleziono. */
export function searchLocation(query: GeoQuery): Promise<GeoLocation[]> {
  const params = new URLSearchParams({
    voivodeship: query.voivodeship,
    city: query.city.trim(),
  });
  // Puste pola pomijamy. Dla Nominatim „ulica: '' " to nie to samo co brak ulicy.
  if (query.district?.trim()) params.set("district", query.district.trim());
  if (query.street?.trim()) params.set("street", query.street.trim());
  if (query.buildingNumber?.trim())
    params.set("buildingNumber", query.buildingNumber.trim());
  if (query.postalCode?.trim())
    params.set("postalCode", query.postalCode.trim());

  return apiFetch<GeoLocation[]>(`/geo/search?${params}`);
}
