import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import {
  createClient,
  fetchClient,
  fetchClientDictionaries,
  updateClient,
  type ClientDictionaries,
} from "../../api/clients";
import { ApiError } from "../../api/client";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { Textarea } from "../../components/ui/Textarea";
import { isValidPhone } from "../../lib/phone";

type Fields = Record<string, string>;

const INITIAL: Fields = {
  firstName: "",
  lastName: "",
  phone: "",
  email: "",
  source: "",
  status: "ACTIVE",
  notes: "",
};

export function ClientFormPage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const [dict, setDict] = useState<ClientDictionaries | null>(null);
  const [fields, setFields] = useState<Fields>(INITIAL);
  // `errors` trzyma tylko błędy z backendu; walidację klienta liczymy na żywo.
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [touched, setTouched] = useState<Set<string>>(new Set());
  const [submitted, setSubmitted] = useState(false);
  const [errorSignal, setErrorSignal] = useState(0);
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetchClientDictionaries().then(setDict).catch(() => undefined);
  }, []);

  // Po nieudanym zapisie przewiń do pierwszego błędnego pola i ustaw kursor.
  useEffect(() => {
    if (errorSignal === 0) return;
    const invalid = document.querySelector<HTMLElement>('[aria-invalid="true"]');
    if (invalid) {
      invalid.scrollIntoView({ behavior: "smooth", block: "center" });
      invalid.focus({ preventScroll: true });
    }
  }, [errorSignal]);

  // Tryb edycji: wczytaj klienta i wypełnij formularz jego danymi.
  useEffect(() => {
    if (!id) return;
    fetchClient(id)
      .then((c) =>
        setFields({
          firstName: c.firstName,
          lastName: c.lastName,
          phone: c.phone ?? "",
          email: c.email ?? "",
          source: c.source ?? "",
          status: c.status,
          notes: c.notes ?? "",
        }),
      )
      .catch(() => setFormError("Nie udało się wczytać klienta do edycji."));
  }, [id]);

  const set = (name: string) => (event: { target: { value: string } }) =>
    setFields((current) => ({ ...current, [name]: event.target.value }));

  // Od chwili opuszczenia pola jego błąd jest widoczny i odświeża się na żywo.
  const markTouched = (key: string) => () =>
    setTouched((current) =>
      current.has(key) ? current : new Set(current).add(key),
    );

  function validate(): Record<string, string> {
    const e: Record<string, string> = {};
    const phone = fields.phone.trim();
    if (phone !== "" && !isValidPhone(phone)) {
      e.phone = "Podaj numer telefonu, np. +48 605 405 932.";
    }
    return e;
  }

  // Wynik walidacji klienta liczony przy każdym renderze — zawsze świeży.
  const liveErrors = validate();

  // Błąd pokazujemy od razu po opuszczeniu pola (lub po próbie zapisu),
  // a nie dopiero po kliknięciu „Zapisz".
  const errorFor = (key: string): string | undefined =>
    errors[key] ??
    (submitted || touched.has(key) ? liveErrors[key] : undefined);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setSubmitted(true);
    setErrors({});
    setFormError(null);

    const fieldErrors = validate();
    if (Object.keys(fieldErrors).length > 0) {
      setFormError("Popraw zaznaczone pola formularza.");
      setErrorSignal((s) => s + 1);
      return;
    }

    setSaving(true);

    try {
      const payload = {
        firstName: fields.firstName,
        lastName: fields.lastName,
        phone: blank(fields.phone),
        email: blank(fields.email),
        source: blank(fields.source),
        status: fields.status,
        notes: blank(fields.notes),
      };

      const saved =
        isEdit && id
          ? await updateClient(id, payload)
          : await createClient(payload);

      navigate(`/klienci/${saved.id}`);
    } catch (cause) {
      if (cause instanceof ApiError) {
        setErrors(cause.fieldErrors ?? {});
        setFormError(cause.message);
      } else {
        setFormError("Nie udało się zapisać klienta.");
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} className="flex max-w-3xl flex-col gap-4 pb-10">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => navigate("/klienci")}
          >
            <ArrowLeft className="size-4" strokeWidth={2} />
            Wróć
          </Button>
          <h1 className="text-base font-semibold tracking-tight text-ink">
            {isEdit ? "Edytuj klienta" : "Nowy klient"}
          </h1>
        </div>

        <Button type="submit" disabled={saving}>
          {saving ? "Zapisywanie…" : "Zapisz klienta"}
        </Button>
      </header>

      {formError && (
        <p className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {formError}
        </p>
      )}

      <section className="card">
        <header className="border-b border-line px-4 py-3">
          <h2 className="text-[13px] font-semibold tracking-tight text-ink">
            Dane klienta
          </h2>
          <p className="mt-0.5 text-[12px] text-ink-muted">
            Rola sprzedający / wynajmujący wynika z ofert powierzonych przez
            klienta — przypiszesz je po zapisaniu, na karcie klienta.
          </p>
        </header>
        <div className="grid gap-4 p-4 md:grid-cols-2">
          <Input
            label="Imię"
            value={fields.firstName}
            onChange={set("firstName")}
            error={errors.firstName}
            required
          />
          <Input
            label="Nazwisko"
            value={fields.lastName}
            onChange={set("lastName")}
            error={errors.lastName}
            required
          />
          <Input
            label="Telefon"
            type="tel"
            inputMode="tel"
            value={fields.phone}
            onChange={(event) =>
              setFields((c) => ({ ...c, phone: event.target.value }))
            }
            onBlur={markTouched("phone")}
            error={errorFor("phone")}
            hint="Np. +48 605 405 932. Wymagany telefon lub e-mail."
            placeholder="+48 605 405 932"
            maxLength={30}
          />
          <Input
            label="E-mail"
            type="email"
            value={fields.email}
            onChange={set("email")}
            error={errors.email}
          />
          <Select
            label="Źródło pozyskania"
            options={dict?.source ?? []}
            value={fields.source}
            onChange={set("source")}
            placeholder="Nie podano"
          />
          <Select
            label="Status"
            options={dict?.status ?? []}
            value={fields.status}
            onChange={set("status")}
          />
          <div className="md:col-span-2">
            <Textarea
              label="Notatki"
              rows={4}
              value={fields.notes}
              onChange={set("notes")}
            />
          </div>
        </div>
      </section>

      <div className="flex justify-end gap-2">
        <Button
          type="button"
          variant="secondary"
          onClick={() => navigate("/klienci")}
        >
          Anuluj
        </Button>
        <Button type="submit" disabled={saving}>
          {saving ? "Zapisywanie…" : "Zapisz klienta"}
        </Button>
      </div>
    </form>
  );
}

const blank = (value: string): string | undefined =>
  value.trim() === "" ? undefined : value.trim();

