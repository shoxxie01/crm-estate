const BASE_URL = import.meta.env.VITE_API_URL ?? "/api";

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly fieldErrors?: Record<string, string>,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

let authToken: string | null = null;

export function setAuthToken(token: string | null) {
  authToken = token;
}

let onUnauthorized: (() => void) | null = null;

/**
 * Wołane, gdy serwer odrzuci *nasz* token. Token żyje 12 h i przestaje być
 * ważny także wtedy, gdy backend wystartuje z innym `DELTA_JWT_SECRET`.
 * Bez tej ścieżki aplikacja zostaje na ekranie zalogowanego użytkownika,
 * a każde żądanie po cichu wraca z 401.
 */
export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler;
}

export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  // Przy FormData nagłówka NIE ustawiamy: przeglądarka dokłada do niego
  // granicę multiparta, a wpisany ręcznie „application/json" albo
  // „multipart/form-data" bez boundary sprawia, że serwer nie rozpozna części.
  if (init.body && !(init.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  if (authToken) headers.set("Authorization", `Bearer ${authToken}`);

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, { ...init, headers });
  } catch {
    throw new ApiError("Nie udało się połączyć z serwerem.", 0);
  }

  if (response.status === 204) return undefined as T;

  const payload = await response.json().catch(() => null);

  if (!response.ok) {
    // 401 przy ustawionym tokenie znaczy, że sesja jest nieważna. Nie że
    // użytkownik podał złe hasło (przy logowaniu tokenu jeszcze nie ma).
    if (response.status === 401 && authToken) {
      onUnauthorized?.();
    }

    // Format zgodny z RFC 7807 (Spring `ProblemDetail`) + mapa błędów pól.
    const message =
      payload?.detail ?? payload?.message ?? "Wystąpił nieoczekiwany błąd.";
    throw new ApiError(message, response.status, payload?.errors);
  }

  return payload as T;
}
