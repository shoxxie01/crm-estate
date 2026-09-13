import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { AppShell } from "./components/layout/AppShell";
import { LoginPage } from "./pages/auth/LoginPage";
import { RegisterPage } from "./pages/auth/RegisterPage";
import { DashboardPage } from "./pages/DashboardPage";
import { PlaceholderPage } from "./pages/PlaceholderPage";
import { CalendarPage } from "./pages/calendar/CalendarPage";
import { ClientsPage } from "./pages/clients/ClientsPage";
import { ClientFormPage } from "./pages/clients/ClientFormPage";
import { ClientDetailPage } from "./pages/clients/ClientDetailPage";
import { PropertiesPage } from "./pages/properties/PropertiesPage";
import { PropertyFormPage } from "./pages/properties/PropertyFormPage";
import { PropertyDetailPage } from "./pages/properties/PropertyDetailPage";
import { InquiriesPage } from "./pages/inquiries/InquiriesPage";
import { IntakePage } from "./pages/public/IntakePage";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/logowanie" element={<LoginPage />} />
          <Route path="/rejestracja" element={<RegisterPage />} />
          {/* Publiczny formularz dla klientów — poza logowaniem i poza powłoką CRM. */}
          <Route path="/zgloszenie/:token" element={<IntakePage />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<AppShell />}>
              <Route index element={<DashboardPage />} />
              <Route path="/kalendarz" element={<CalendarPage />} />
              <Route path="/nieruchomosci" element={<PropertiesPage />} />
              <Route path="/nieruchomosci/nowa" element={<PropertyFormPage />} />
              <Route
                path="/nieruchomosci/:id"
                element={<PropertyDetailPage />}
              />
              <Route
                path="/nieruchomosci/:id/edytuj"
                element={<PropertyFormPage />}
              />
              <Route path="/klienci" element={<ClientsPage />} />
              <Route path="/klienci/nowy" element={<ClientFormPage />} />
              <Route path="/klienci/:id" element={<ClientDetailPage />} />
              <Route path="/klienci/:id/edytuj" element={<ClientFormPage />} />
              <Route path="/zgloszenia" element={<InquiriesPage />} />
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
