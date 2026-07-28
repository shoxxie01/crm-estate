import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { AlertCircle, Eye, EyeOff } from "lucide-react";
import { AuthLayout } from "./AuthLayout";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { useAuth } from "../../auth/AuthContext";
import { ApiError } from "../../api/client";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function validate() {
    const errors: Record<string, string> = {};
    if (!email.trim()) errors.email = "Podaj adres e-mail.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
      errors.email = "To nie wygląda na poprawny adres e-mail.";
    if (!password) errors.password = "Podaj hasło.";
    return errors;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);

    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    try {
      await login({ email, password });
      const from = (location.state as { from?: Location } | null)?.from;
      navigate(from?.pathname ?? "/", { replace: true });
    } catch (error) {
      if (error instanceof ApiError) {
        setFormError(error.message);
        if (error.fieldErrors) setFieldErrors(error.fieldErrors);
      } else {
        setFormError("Wystąpił nieoczekiwany błąd. Spróbuj ponownie.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Zaloguj się"
      subtitle="Wpisz dane swojego konta, aby wejść do Delta CRM."
      footer={
        <>
          Nie masz jeszcze konta?{" "}
          <Link
            to="/rejestracja"
            className="font-medium text-accent hover:underline"
          >
            Załóż konto biura
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
        {formError && (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-md border border-critical/30 bg-critical/6 px-3 py-2.5 text-[13px] text-critical"
          >
            <AlertCircle className="mt-px size-4 shrink-0" strokeWidth={2} />
            <span>{formError}</span>
          </div>
        )}

        <Input
          label="Adres e-mail"
          type="email"
          autoComplete="email"
          placeholder="imie.nazwisko@biuro.pl"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={fieldErrors.email}
        />

        <div>
          <Input
            label="Hasło"
            type={showPassword ? "text" : "password"}
            autoComplete="current-password"
            placeholder="••••••••"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            error={fieldErrors.password}
            trailing={
              <button
                type="button"
                onClick={() => setShowPassword((visible) => !visible)}
                aria-label={showPassword ? "Ukryj hasło" : "Pokaż hasło"}
                className="rounded p-1.5 text-ink-muted hover:bg-subtle hover:text-ink-secondary"
              >
                {showPassword ? (
                  <EyeOff className="size-4" strokeWidth={1.75} />
                ) : (
                  <Eye className="size-4" strokeWidth={1.75} />
                )}
              </button>
            }
          />

          <div className="mt-2.5 flex items-center justify-between">
            <label className="flex items-center gap-2 text-[13px] text-ink-secondary">
              <input
                type="checkbox"
                name="remember"
                className="size-3.5 rounded-sm border-line-strong accent-accent"
              />
              Zapamiętaj mnie
            </label>
            <a
              href="#reset"
              className="text-[13px] text-ink-secondary hover:text-accent hover:underline"
            >
              Nie pamiętasz hasła?
            </a>
          </div>
        </div>

        <Button type="submit" fullWidth disabled={submitting} className="mt-1">
          {submitting ? "Logowanie…" : "Zaloguj się"}
        </Button>
      </form>
    </AuthLayout>
  );
}
