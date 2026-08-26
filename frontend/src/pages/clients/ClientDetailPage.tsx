import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Link2, Pencil, Trash2, X } from "lucide-react";
import {
  assignProperty,
  deleteClient,
  fetchClient,
  unassignProperty,
  type ClientDetail,
} from "../../api/clients";
import {
  fetchProperties,
  type PropertySummary,
} from "../../api/properties";
import { ApiError } from "../../api/client";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Select } from "../../components/ui/Select";
import { formatCurrency, formatNumber } from "../../lib/format";
import { EventTimeline } from "../calendar/EventTimeline";
import { ClientIntent } from "./intent";

export function ClientDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [client, setClient] = useState<ClientDetail | null>(null);
  const [agencyProperties, setAgencyProperties] = useState<PropertySummary[]>([]);
  const [selected, setSelected] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    fetchClient(id)
      .then((result) => {
        setClient(result);
        setError(null);
      })
      .catch((cause) =>
        setError(
          cause instanceof ApiError
            ? cause.message
            : "Nie udało się wczytać klienta.",
        ),
      )
      .finally(() => setLoading(false));
  }, [id]);

  // Oferty biura do wyboru przy przypinaniu właściciela.
  useEffect(() => {
    fetchProperties()
      .then((page) => setAgencyProperties(page.content))
      .catch(() => undefined);
  }, []);

  // Nie proponujemy ofert już przypisanych do tego klienta.
  const assignable = useMemo(() => {
    const owned = new Set(client?.ownedProperties.map((p) => p.id));
    return agencyProperties.filter((p) => !owned.has(p.id));
  }, [agencyProperties, client]);

  async function attach() {
    if (!client || !selected) return;
    setBusy(true);
    setError(null);
    try {
      const updated = await assignProperty(client.id, selected);
      setClient(updated);
      setSelected("");
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Nie udało się przypisać oferty.",
      );
    } finally {
      setBusy(false);
    }
  }

  async function detach(propertyId: string) {
    if (!client) return;
    setBusy(true);
    setError(null);
    try {
      await unassignProperty(client.id, propertyId);
      setClient(await fetchClient(client.id));
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Nie udało się odpiąć oferty.",
      );
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!client) return;
    setDeleting(true);
    setError(null);
    try {
      await deleteClient(client.id);
      navigate("/klienci");
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Nie udało się usunąć klienta.",
      );
      setDeleting(false);
    }
  }

  if (loading) {
    return <p className="text-[13px] text-ink-muted">Wczytywanie…</p>;
  }

  if (!client) {
    return (
      <div className="flex flex-col gap-3">
        <p className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {error ?? "Nie znaleziono klienta."}
        </p>
        <Button variant="secondary" size="sm" onClick={() => navigate("/klienci")}>
          <ArrowLeft className="size-4" strokeWidth={2} />
          Wróć do listy
        </Button>
      </div>
    );
  }

  return (
    <div className="flex max-w-4xl flex-col gap-4">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate("/klienci")}
          >
            <ArrowLeft className="size-4" strokeWidth={2} />
            Wróć
          </Button>
          <h1 className="text-base font-semibold tracking-tight text-ink">
            {client.firstName} {client.lastName}
          </h1>
          <ClientIntent
            sellCount={client.sellCount}
            rentCount={client.rentCount}
          />
        </div>

        <div className="flex items-center gap-2">
          <Badge tone={client.status === "ACTIVE" ? "good" : "neutral"}>
            {client.status === "ACTIVE" ? "Aktywny" : "Archiwalny"}
          </Badge>

          <Button
            variant="secondary"
            size="sm"
            onClick={() => navigate(`/klienci/${client.id}/edytuj`)}
            disabled={busy || deleting}
          >
            <Pencil className="size-4" strokeWidth={2} />
            Edytuj
          </Button>

          {confirmDelete ? (
            <span className="flex items-center gap-2 text-[13px] text-ink-secondary">
              Usunąć?
              <Button
                size="sm"
                variant="secondary"
                onClick={remove}
                disabled={deleting}
                className="border-critical/40 text-critical hover:bg-critical/8"
              >
                {deleting ? "Usuwanie…" : "Tak, usuń"}
              </Button>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => setConfirmDelete(false)}
                disabled={deleting}
              >
                Anuluj
              </Button>
            </span>
          ) : (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setConfirmDelete(true)}
              disabled={busy}
              className="text-critical hover:bg-critical/8"
            >
              <Trash2 className="size-4" strokeWidth={2} />
              Usuń
            </Button>
          )}
        </div>
      </header>

      {client.ownedProperties.length > 0 && (
        <p className="text-[12px] text-ink-muted">
          Usunięcie klienta odepnie jego oferty (pozostaną w systemie bez
          właściciela).
        </p>
      )}

      {error && (
        <p className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {error}
        </p>
      )}

      <section className="card">
        <header className="border-b border-line px-4 py-3">
          <h2 className="text-[13px] font-semibold tracking-tight text-ink">
            Dane kontaktowe
          </h2>
        </header>
        <dl className="grid gap-x-8 gap-y-3 p-4 text-[13px] sm:grid-cols-2">
          <Field label="Telefon" value={client.phone} mono />
          <Field label="E-mail" value={client.email} />
          <Field label="Opiekun" value={client.agentName} />
          <Field label="Notatki" value={client.notes} />
        </dl>
      </section>

      <section className="card overflow-hidden">
        <header className="flex flex-wrap items-end justify-between gap-3 border-b border-line px-4 py-3">
          <div>
            <h2 className="text-[13px] font-semibold tracking-tight text-ink">
              Powierzone oferty
            </h2>
            <p className="mt-0.5 text-[12px] text-ink-muted">
              To one decydują, czy klient jest sprzedającym, czy wynajmującym.
            </p>
          </div>
          <div className="flex items-end gap-2">
            <Select
              label="Przypisz ofertę"
              placeholder={
                assignable.length ? "Wybierz ofertę…" : "Brak wolnych ofert"
              }
              options={assignable.map((p) => ({
                value: p.id,
                label: `${p.referenceNumber} — ${p.title}`,
              }))}
              value={selected}
              onChange={(event) => setSelected(event.target.value)}
              disabled={busy || assignable.length === 0}
              className="w-64"
            />
            <Button
              type="button"
              variant="secondary"
              onClick={attach}
              disabled={busy || !selected}
            >
              <Link2 className="size-4" strokeWidth={2} />
              Przypisz
            </Button>
          </div>
        </header>

        {client.ownedProperties.length === 0 ? (
          <p className="px-4 py-8 text-center text-[13px] text-ink-secondary">
            Klient nie ma jeszcze powierzonych ofert.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] border-collapse text-[13px]">
              <thead>
                <tr className="border-b border-line-strong text-left text-[11px] font-medium tracking-wide text-ink-muted uppercase">
                  <th className="px-4 py-2.5 font-medium">Numer</th>
                  <th className="px-4 py-2.5 font-medium">Oferta</th>
                  <th className="px-4 py-2.5 font-medium">Transakcja</th>
                  <th className="px-4 py-2.5 text-right font-medium">Cena</th>
                  <th className="px-4 py-2.5" />
                </tr>
              </thead>
              <tbody>
                {client.ownedProperties.map((property) => (
                  <tr
                    key={property.id}
                    className="border-b border-line last:border-0 hover:bg-subtle"
                  >
                    <td className="px-4 py-2.5 font-mono text-[12px] text-ink-secondary">
                      {property.referenceNumber}
                    </td>
                    <td className="px-4 py-2.5">
                      <Link
                        to={`/nieruchomosci`}
                        className="font-medium text-ink hover:text-accent"
                      >
                        {property.title}
                      </Link>
                      <div className="text-[12px] text-ink-muted">
                        {property.city}
                        {property.district && `, ${property.district}`}
                      </div>
                    </td>
                    <td className="px-4 py-2.5">
                      <Badge
                        tone={
                          property.transactionType === "SALE"
                            ? "accent"
                            : "warning"
                        }
                      >
                        {property.transactionType === "SALE"
                          ? "Sprzedaż"
                          : "Wynajem"}
                      </Badge>
                    </td>
                    <td className="px-4 py-2.5 text-right font-medium tabular-nums text-ink">
                      {formatCurrency(property.price)}
                      <span className="ml-1 text-[12px] font-normal text-ink-muted">
                        · {formatNumber(property.totalArea)} m²
                      </span>
                    </td>
                    <td className="px-4 py-2.5 text-right">
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={() => detach(property.id)}
                        disabled={busy}
                        aria-label="Odepnij ofertę"
                      >
                        <X className="size-4" strokeWidth={2} />
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Terminy z kalendarza. Historia kontaktu z klientem trzyma się jego karty,
          a nie pamięci agenta — po to jest powiązanie calendar_events.client_id. */}
      <section className="card">
        <header className="border-b border-line px-4 py-3">
          <h2 className="text-[13px] font-semibold tracking-tight text-ink">
            Terminy
          </h2>
        </header>
        <div className="p-4">
          <EventTimeline clientId={client.id} />
        </div>
      </section>
    </div>
  );
}

function Field({
  label,
  value,
  mono,
}: {
  label: string;
  value: string | null;
  mono?: boolean;
}) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-[11px] font-medium tracking-wide text-ink-muted uppercase">
        {label}
      </dt>
      <dd className={mono ? "tabular-nums text-ink" : "text-ink"}>
        {value ? value : <span className="text-ink-muted">—</span>}
      </dd>
    </div>
  );
}
