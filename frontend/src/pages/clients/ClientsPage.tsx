import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Plus, Users } from "lucide-react";
import {
  fetchClientDictionaries,
  fetchClients,
  type ClientDictionaries,
  type ClientSummary,
} from "../../api/clients";
import { type PageResponse } from "../../api/properties";
import { ApiError } from "../../api/client";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { formatNumber } from "../../lib/format";
import { ClientIntent } from "./intent";

export function ClientsPage() {
  const navigate = useNavigate();
  const [dictionaries, setDictionaries] = useState<ClientDictionaries | null>(
    null,
  );
  const [page, setPage] = useState<PageResponse<ClientSummary> | null>(null);
  const [status, setStatus] = useState("");
  const [search, setSearch] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchClientDictionaries().then(setDictionaries).catch(() => undefined);
  }, []);

  // Odpytujemy dopiero po chwili od ostatniego znaku — bez tego każde wciśnięcie
  // klawisza to osobne żądanie do backendu.
  useEffect(() => {
    const timer = setTimeout(() => {
      setLoading(true);
      fetchClients({
        status: status || undefined,
        search: search.trim() || undefined,
      })
        .then((result) => {
          setPage(result);
          setError(null);
        })
        .catch((cause) =>
          setError(
            cause instanceof ApiError
              ? cause.message
              : "Nie udało się wczytać klientów.",
          ),
        )
        .finally(() => setLoading(false));
    }, 250);

    return () => clearTimeout(timer);
  }, [status, search]);

  const sourceLabels = useMemo(() => {
    const map = new Map<string, string>();
    for (const entry of dictionaries?.source ?? []) map.set(entry.value, entry.label);
    return map;
  }, [dictionaries]);

  return (
    <div className="flex flex-col gap-4">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-base font-semibold tracking-tight text-ink">
            Klienci
          </h1>
          <p className="mt-0.5 text-[13px] text-ink-secondary">
            {page
              ? `${formatNumber(page.totalElements)} ${clientForm(page.totalElements)} w bazie biura`
              : "Wczytywanie…"}
          </p>
        </div>

        <div className="flex items-end gap-2">
          <Input
            label="Szukaj"
            placeholder="Nazwisko, telefon, e-mail…"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            className="w-56"
          />
          <Select
            label="Status"
            placeholder="Wszyscy"
            options={dictionaries?.status ?? []}
            value={status}
            onChange={(event) => setStatus(event.target.value)}
            className="w-40"
          />
          <Link to="/klienci/nowy">
            <Button>
              <Plus className="size-4" strokeWidth={2} />
              Dodaj klienta
            </Button>
          </Link>
        </div>
      </header>

      {error && (
        <p className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {error}
        </p>
      )}

      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[820px] border-collapse text-[13px]">
            <thead>
              <tr className="border-b border-line-strong text-left text-[11px] font-medium tracking-wide text-ink-muted uppercase">
                <th className="px-4 py-2.5 font-medium">Klient</th>
                <th className="px-4 py-2.5 font-medium">Kontakt</th>
                <th className="px-4 py-2.5 font-medium">Źródło</th>
                <th className="px-4 py-2.5 font-medium">Rola</th>
                <th className="px-4 py-2.5 font-medium">Opiekun</th>
                <th className="px-4 py-2.5 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={6} className="px-4 py-10 text-center text-ink-muted">
                    Wczytywanie…
                  </td>
                </tr>
              )}

              {!loading && page?.content.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center">
                    <Users
                      className="mx-auto mb-2 size-6 text-ink-muted"
                      strokeWidth={1.5}
                    />
                    <p className="text-[13px] text-ink-secondary">
                      Brak klientów spełniających kryteria.
                    </p>
                  </td>
                </tr>
              )}

              {!loading &&
                page?.content.map((client) => (
                  <tr
                    key={client.id}
                    onClick={() => navigate(`/klienci/${client.id}`)}
                    className="cursor-pointer border-b border-line last:border-0 hover:bg-subtle"
                  >
                    <td className="px-4 py-2.5 font-medium text-ink">
                      {client.firstName} {client.lastName}
                    </td>
                    <td className="px-4 py-2.5 text-ink-secondary">
                      {client.phone && (
                        <div className="tabular-nums">{client.phone}</div>
                      )}
                      {client.email && (
                        <div className="text-[12px] text-ink-muted">
                          {client.email}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-2.5 text-ink-secondary">
                      {client.source
                        ? (sourceLabels.get(client.source) ?? client.source)
                        : "—"}
                    </td>
                    <td className="px-4 py-2.5">
                      <ClientIntent
                        sellCount={client.sellCount}
                        rentCount={client.rentCount}
                        buyerCount={client.buyerCount}
                        tenantCount={client.tenantCount}
                      />
                    </td>
                    <td className="px-4 py-2.5 text-ink-secondary">
                      {client.agentName}
                    </td>
                    <td className="px-4 py-2.5">
                      <Badge
                        tone={client.status === "ACTIVE" ? "good" : "neutral"}
                      >
                        {client.status === "ACTIVE" ? "Aktywny" : "Archiwalny"}
                      </Badge>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function clientForm(count: number): string {
  if (count === 1) return "klient";
  const rest = count % 10;
  const teens = count % 100;
  if (rest >= 2 && rest <= 4 && (teens < 12 || teens > 14)) return "klienci";
  return "klientów";
}
