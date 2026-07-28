import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";

export function ProtectedRoute() {
  const { status } = useAuth();
  const location = useLocation();

  if (status === "loading") {
    return (
      <div className="grid min-h-dvh place-items-center text-ink-muted">
        Ładowanie…
      </div>
    );
  }

  if (status === "anonymous") {
    return <Navigate to="/logowanie" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
