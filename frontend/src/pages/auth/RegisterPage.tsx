import { useMemo, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AlertCircle, Eye, EyeOff } from "lucide-react";
import { AuthLayout } from "./AuthLayout";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { useAuth } from "../../auth/AuthContext";
import { ApiError } from "../../api/client";

const STRENGTH_LEVELS = [
  { label: "Zbyt krótkie", color: "var(--color-critical)" },
  { label: "Słabe", color: "var(--color-critical)" },
  { label: "Średnie", color: "var(--color-warning)" },
  { label: "Dobre", color: "var(--color-good)" },
  { label: "Silne", color: "var(--color-good)" },
];

function scorePassword(password: string): number {
  if (password.length < 8) return password.length === 0 ? 0 : 1;
  let score = 2;
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score += 1;
  if (/\d/.test(password) && /[^\w\s]/.test(password)) score += 1;
  return Math.min(score, 4);
}

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    agencyName: "",
    email: "",
    password: "",
  });
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const strength = useMemo(() => scorePassword(form.password), [form.password]);

  const update = (key: keyof typeof form) => (event: { target: { value: string } }) =>
    setForm((current) => ({ ...current, [key]: event.target.value }));

  function validate() {
    const errors: Record<string, string> = {};
    if (!form.firstName.trim()) errors.firstName = "Podaj imię.";
    if (!form.lastName.trim()) errors.lastName = "Podaj nazwisko.";
    if (!form.agencyName.trim()) errors.agencyName = "Podaj nazwę biura.";
    if (!form.email.trim()) errors.email = "Podaj adres e-mail.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))
      errors.email = "To nie wygląda na poprawny adres e-mail.";
    if (form.password.length < 8)
      errors.password = "Hasło musi mieć co najmniej 8 znaków.";
    if (!acceptedTerms)
      errors.terms = "Zaakceptuj regulamin, aby założyć konto.";
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
      await register(form);
      navigate("/", { replace: true });
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
      title="Załóż konto biura"
      subtitle="Utwórz konto administratora — współpracowników dodasz później."
      footer={
        <>
          Masz już konto?{" "}
          <Link to="/logowanie" className="font-medium text-accent hover:underline">
            Zaloguj się
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

        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Imię"
            autoComplete="given-name"
            value={form.firstName}
            onChange={update("firstName")}
            error={fieldErrors.firstName}
          />
          <Input
            label="Nazwisko"
            autoComplete="family-name"
            value={form.lastName}
            onChange={update("lastName")}
            error={fieldErrors.lastName}
          />
        </div>

        <Input
          label="Nazwa biura"
          autoComplete="organization"
          placeholder="Delta Nieruchomości"
          value={form.agencyName}
          onChange={update("agencyName")}
          error={fieldErrors.agencyName}
        />

        <Input
          label="Służbowy e-mail"
          type="email"
          autoComplete="email"
          placeholder="imie.nazwisko@biuro.pl"
          value={form.email}
          onChange={update("email")}
          error={fieldErrors.email}
        />

        <div>
          <Input
            label="Hasło"
            type={showPassword ? "text" : "password"}
            autoComplete="new-password"
            value={form.password}
            onChange={update("password")}
            error={fieldErrors.password}
            hint="Minimum 8 znaków. Wielkie i małe litery oraz cyfra znacząco podnoszą siłę."
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

          {form.password.length > 0 && (
            <div className="mt-2 flex items-center gap-2">
              <div
                className="flex h-1 flex-1 gap-1"
                role="meter"
                aria-valuenow={strength}
                aria-valuemin={0}
                aria-valuemax={4}
                aria-label="Siła hasła"
              >
                {[1, 2, 3, 4].map((step) => (
                  <span
                    key={step}
                    className="flex-1 rounded-full bg-subtle"
                    style={
                      step <= strength
                        ? { backgroundColor: STRENGTH_LEVELS[strength].color }
                        : undefined
                    }
                  />
                ))}
              </div>
              <span className="text-[12px] text-ink-secondary">
                {STRENGTH_LEVELS[strength].label}
              </span>
            </div>
          )}
        </div>

        <div>
          <label className="flex items-start gap-2 text-[13px] text-ink-secondary">
            <input
              type="checkbox"
              checked={acceptedTerms}
              onChange={(event) => setAcceptedTerms(event.target.checked)}
              className="mt-0.5 size-3.5 shrink-0 rounded-sm border-line-strong accent-accent"
            />
            <span>
              Akceptuję{" "}
              <a href="#regulamin" className="text-accent hover:underline">
                regulamin
              </a>{" "}
              i{" "}
              <a href="#rodo" className="text-accent hover:underline">
                zasady przetwarzania danych
              </a>
              .
            </span>
          </label>
          {fieldErrors.terms && (
            <p className="mt-1 text-[12px] text-critical">{fieldErrors.terms}</p>
          )}
        </div>

        <Button type="submit" fullWidth disabled={submitting} className="mt-1">
          {submitting ? "Tworzenie konta…" : "Załóż konto"}
        </Button>
      </form>
    </AuthLayout>
  );
}
