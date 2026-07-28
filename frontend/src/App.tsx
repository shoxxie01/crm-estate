import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { AppShell } from "./components/layout/AppShell";
import { LoginPage } from "./pages/auth/LoginPage";
import { RegisterPage } from "./pages/auth/RegisterPage";
import { DashboardPage } from "./pages/DashboardPage";
import { PlaceholderPage } from "./pages/PlaceholderPage";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/logowanie" element={<LoginPage />} />
          <Route path="/rejestracja" element={<RegisterPage />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<AppShell />}>
              <Route index element={<DashboardPage />} />
              <Route path="/kalendarz" element={<PlaceholderPage />} />
              <Route path="/nieruchomosci" element={<PlaceholderPage />} />
              <Route path="/klienci" element={<PlaceholderPage />} />
              <Route path="/umowy" element={<PlaceholderPage />} />
              <Route path="/eksport" element={<PlaceholderPage />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
