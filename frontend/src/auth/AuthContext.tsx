import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  login as loginRequest,
  register as registerRequest,
  type AuthResponse,
  type LoginPayload,
  type RegisterPayload,
  type User,
} from "../api/auth";
import { setAuthToken } from "../api/client";

const STORAGE_KEY = "delta-crm.session";

interface StoredSession {
  token: string;
  user: User;
}

interface AuthContextValue {
  user: User | null;
  status: "loading" | "authenticated" | "anonymous";
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function readSession(): StoredSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredSession) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [status, setStatus] = useState<AuthContextValue["status"]>("loading");

  useEffect(() => {
    const session = readSession();
    if (session) {
      setAuthToken(session.token);
      setUser(session.user);
      setStatus("authenticated");
    } else {
      setStatus("anonymous");
    }
  }, []);

  const persist = useCallback((response: AuthResponse) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(response));
    setAuthToken(response.token);
    setUser(response.user);
    setStatus("authenticated");
  }, []);

  const login = useCallback(
    async (payload: LoginPayload) => persist(await loginRequest(payload)),
    [persist],
  );

  const register = useCallback(
    async (payload: RegisterPayload) => persist(await registerRequest(payload)),
    [persist],
  );

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY);
    setAuthToken(null);
    setUser(null);
    setStatus("anonymous");
  }, []);

  const value = useMemo(
    () => ({ user, status, login, register, logout }),
    [user, status, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth musi być użyte wewnątrz AuthProvider.");
  return context;
}
