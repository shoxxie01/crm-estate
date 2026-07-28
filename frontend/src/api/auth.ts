import { apiFetch, ApiError } from "./client";

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: "AGENT" | "MANAGER" | "ADMIN";
  agencyName: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  agencyName: string;
}

/**
 * Domyślnie uderzamy w prawdziwe /api/auth/** (Spring Boot). Mock zostaje na
 * wypadek pracy nad frontem bez uruchomionego backendu: VITE_USE_MOCK_AUTH=true.
 */
const USE_MOCK = import.meta.env.VITE_USE_MOCK_AUTH === "true";

const MOCK_USER: User = {
  id: "u_1",
  email: "anna.kowalska@delta.pl",
  firstName: "Anna",
  lastName: "Kowalska",
  role: "MANAGER",
  agencyName: "Delta Nieruchomości",
};

const delay = (ms: number) => new Promise((r) => setTimeout(r, ms));

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  if (USE_MOCK) {
    await delay(600);
    if (payload.password.length < 8) {
      throw new ApiError("Nieprawidłowy e-mail lub hasło.", 401);
    }
    return { token: "mock-token", user: { ...MOCK_USER, email: payload.email } };
  }
  return apiFetch<AuthResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function register(
  payload: RegisterPayload,
): Promise<AuthResponse> {
  if (USE_MOCK) {
    await delay(700);
    if (payload.email.endsWith("@example.com")) {
      throw new ApiError("Konto z tym adresem już istnieje.", 409, {
        email: "Ten adres e-mail jest już zajęty.",
      });
    }
    return {
      token: "mock-token",
      user: {
        ...MOCK_USER,
        email: payload.email,
        firstName: payload.firstName,
        lastName: payload.lastName,
        agencyName: payload.agencyName,
        role: "ADMIN",
      },
    };
  }
  return apiFetch<AuthResponse>("/auth/register", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function fetchCurrentUser(): Promise<User> {
  if (USE_MOCK) {
    await delay(150);
    return MOCK_USER;
  }
  return apiFetch<User>("/auth/me");
}
