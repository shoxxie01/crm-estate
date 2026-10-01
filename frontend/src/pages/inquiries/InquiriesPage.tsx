import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  Check,
  ChevronDown,
  Copy,
  ExternalLink,
  Inbox,
  Mail,
  MapPin,
  Phone,
  RefreshCw,
  UserPlus,
  Users,
  X,
} from "lucide-react";
import {
  INQUIRIES_CHANGED,
  convertInquiry,
  fetchInquiries,
  fetchInquiryMatches,
  fetchIntakeLink,
  regenerateIntakeLink,
  rejectInquiry,
  type Inquiry,
  type SaleOffer,
} from "../../api/inquiries";
import type { PropertyMatch } from "../../api/matching";
import { ApiError } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { MatchCriteria } from "../../components/MatchCriteria";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { ConfirmDialog } from "../../components/ui/ConfirmDialog";
import { cn } from "../../lib/cn";
import { formatCurrency, formatNumber } from "../../lib/format";
import {
  RequirementFacts,
  RequirementHeadline,
  useRequirementDictionaries,
  type RequirementLabels,
} from "../clients/RequirementDetails";

type Tab = "NEW" | "CONVERTED";

const dateTime = (value: string) =>
  new Date(value).toLocaleString("pl-PL", { dateStyle: "short", timeStyle: "short" });

/**
 * Skrzynka zgłoszeń z publicznego formularza. Nic stąd nie trafia do bazy
 * klientów samo. Agent przyjmuje zgłoszenie (nowy klient albo dopięcie do
 * istniejącego) albo je odrzuca, co usuwa dane osobowe zgłaszającego.
 */
export function InquiriesPage() {
  const { labels } = useRequirementDictionaries();
  const [tab, setTab] = useState<Tab>("NEW");
  const [items, setItems] = useState<Inquiry[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<{ name: string; clientId: string } | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [rejecting, setRejecting] = useState<Inquiry | null>(null);

  const load = useCallback(() => {
    setItems(null);
    fetchInquiries(tab)
      .then((result) => {
        setItems(result);
        setError(null);
      })
      .catch((cause) =>
        setError(cause instanceof ApiError ? cause.message : "Nie udało się wczytać zgłoszeń."),
      );
  }, [tab]);

  useEffect(load, [load]);

  const changed = () => {
    window.dispatchEvent(new Event(INQUIRIES_CHANGED));
    load();
  };

  async function convert(inquiry: Inquiry, clientId?: string) {
    setBusyId(inquiry.id);
    setError(null);
    setSuccess(null);
    try {
      const result = await convertInquiry(inquiry.id, clientId);
      setSuccess({ name: `${inquiry.firstName} ${inquiry.lastName}`, clientId: result.clientId });
      changed();
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Nie udało się przyjąć zgłoszenia.");
    } finally {
      setBusyId(null);
    }
  }

  async function reject(inquiry: Inquiry) {
    setBusyId(inquiry.id);
    setError(null);
    setSuccess(null);
    try {
      await rejectInquiry(inquiry.id);
      setRejecting(null);
      changed();
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Nie udało się odrzucić zgłoszenia.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="flex max-w-4xl flex-col gap-4">
      <header>
        <h1 className="text-base font-semibold tracking-tight text-ink">Zgłoszenia</h1>
        <p className="mt-0.5 text-[13px] text-ink-secondary">
          Klienci, którzy przez formularz biura opisali, co chcą kupić albo sprzedać.
        </p>
      </header>

      <IntakeLinkCard />

      <div className="flex gap-1 border-b border-line" role="tablist">
        {(
          [
            ["NEW", "Nowe"],
            ["CONVERTED", "Przyjęte"],
          ] as const
        ).map(([value, label]) => (
          <button
            key={value}
            role="tab"
            aria-selected={tab === value}
            onClick={() => {
              setTab(value);
              setSuccess(null);
            }}
            className={cn(
              "-mb-px border-b-2 px-3 py-2 text-[13px] font-medium transition-colors",
              tab === value
                ? "border-accent text-accent"
                : "border-transparent text-ink-secondary hover:text-ink",
            )}
          >
            {label}
          </button>
        ))}
      </div>

      {error && (
        <p className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {error}
        </p>
      )}
      {success && (
        <p className="flex flex-wrap items-center gap-1.5 rounded-md border border-good/30 bg-good/8 px-3 py-2 text-[13px] text-good-ink">
          <Check className="size-4" strokeWidth={2.25} />
          Zgłoszenie {success.name} przyjęte.
          <Link to={`/klienci/${success.clientId}`} className="font-medium underline">
            Otwórz kartę klienta
          </Link>
        </p>
      )}

      {items === null ? (
        !error && <p className="text-[13px] text-ink-muted">Wczytywanie…</p>
      ) : items.length === 0 ? (
        <div className="card px-4 py-12 text-center">
          <Inbox className="mx-auto mb-2 size-6 text-ink-muted" strokeWidth={1.5} />
          <p className="text-[13px] text-ink-secondary">
            {tab === "NEW" ? "Brak nowych zgłoszeń." : "Nie przyjęto jeszcze żadnego zgłoszenia."}
          </p>
          {tab === "NEW" && (
            <p className="mt-0.5 text-[12px] text-ink-muted">
              Wyślij klientom link do formularza albo wstaw go na stronę biura.
            </p>
          )}
        </div>
      ) : (
        <ul className="flex flex-col gap-3">
          {items.map((inquiry) => (
            <li key={inquiry.id}>
              <InquiryCard
                inquiry={inquiry}
                labels={labels}
                busy={busyId === inquiry.id}
                onConvert={(clientId) => convert(inquiry, clientId)}
                onReject={() => setRejecting(inquiry)}
              />
            </li>
          ))}
        </ul>
      )}

      {rejecting && (
        <ConfirmDialog
          title="Odrzucić zgłoszenie?"
          description={`Zgłoszenie ${rejecting.firstName} ${rejecting.lastName} zostanie usunięte razem z danymi kontaktowymi. Tego nie da się cofnąć.`}
          confirmLabel="Odrzuć i usuń"
          busy={busyId === rejecting.id}
          onCancel={() => setRejecting(null)}
          onConfirm={() => reject(rejecting)}
        />
      )}
    </div>
  );
}

function IntakeLinkCard() {
  const { user } = useAuth();
  const [token, setToken] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchIntakeLink()
      .then((r) => setToken(r.token))
      .catch(() => setError("Nie udało się wczytać linku do formularza."));
  }, []);

  const url = token ? `${window.location.origin}/zgloszenie/${token}` : "";

  async function copy() {
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setError("Przeglądarka nie pozwoliła skopiować. Zaznacz link i skopiuj ręcznie.");
    }
  }

  async function regenerate() {
    setRegenerating(true);
    setError(null);
    try {
      const r = await regenerateIntakeLink();
      setToken(r.token);
      setConfirming(false);
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Nie udało się wygenerować linku.");
    } finally {
      setRegenerating(false);
    }
  }

  return (
    <section className="card px-4 py-3">
      <div className="flex flex-wrap items-end gap-2">
        <label className="flex min-w-0 flex-1 flex-col gap-1.5">
          <span className="text-[13px] font-medium text-ink">Link do formularza dla klientów</span>
          <input
            readOnly
            value={url || "Wczytywanie…"}
            onFocus={(e) => e.target.select()}
            className="h-9 w-full min-w-0 rounded-md border border-line bg-subtle px-3 font-mono text-[12px] text-ink-secondary focus:border-accent focus:ring-2 focus:ring-accent-ring/50 focus:outline-none"
          />
        </label>
        <Button type="button" variant="secondary" onClick={copy} disabled={!token}>
          {copied ? <Check className="size-4" strokeWidth={2} /> : <Copy className="size-4" strokeWidth={2} />}
          {copied ? "Skopiowano" : "Kopiuj"}
        </Button>
        <a
          href={url || undefined}
          target="_blank"
          rel="noreferrer"
          className={cn(
            "inline-flex h-9 items-center gap-2 rounded-md border border-line bg-surface px-3.5 text-sm font-medium text-ink hover:border-line-strong hover:bg-subtle",
            !token && "pointer-events-none opacity-55",
          )}
        >
          <ExternalLink className="size-4" strokeWidth={2} />
          Podgląd
        </a>
      </div>
      <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
        <p className="text-[12px] text-ink-muted">
          Wyślij go klientowi, wstaw na stronę biura albo do ogłoszenia. Zgłoszenia trafią tutaj.
        </p>
        {user?.role === "ADMIN" && (
          <button
            type="button"
            onClick={() => setConfirming(true)}
            className="inline-flex items-center gap-1 text-[12px] text-ink-secondary hover:text-ink"
          >
            <RefreshCw className="size-3.5" strokeWidth={2} />
            Wygeneruj nowy link
          </button>
        )}
      </div>
      {error && <p className="mt-2 text-[12px] text-critical">{error}</p>}

      {confirming && (
        <ConfirmDialog
          title="Wygenerować nowy link?"
          description="Dotychczasowy link przestanie działać od razu. Także tam, gdzie jest już wstawiony. Użyj tego, gdy do formularza zaczął trafiać spam."
          confirmLabel="Wygeneruj"
          busyLabel="Generowanie…"
          busy={regenerating}
          onCancel={() => setConfirming(false)}
          onConfirm={regenerate}
        />
      )}
    </section>
  );
}

function InquiryCard({
  inquiry: q,
  labels,
  busy,
  onConvert,
  onReject,
}: {
  inquiry: Inquiry;
  labels: RequirementLabels;
  busy: boolean;
  onConvert: (clientId?: string) => void;
  onReject: () => void;
}) {
  const isNew = q.status === "NEW";

  return (
    <article className="card p-4">
      <header className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2">
        <div className="min-w-0">
          <h2 className="text-[14px] font-semibold tracking-tight text-ink">
            {q.firstName} {q.lastName}
          </h2>
          <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-[12px] text-ink-secondary">
            {q.phone && (
              <a
                href={`tel:${q.phone.replace(/\s/g, "")}`}
                className="inline-flex items-center gap-1 tabular-nums hover:text-accent"
              >
                <Phone className="size-3" strokeWidth={2} />
                {q.phone}
              </a>
            )}
            {q.email && (
              <a href={`mailto:${q.email}`} className="inline-flex items-center gap-1 hover:text-accent">
                <Mail className="size-3" strokeWidth={2} />
                {q.email}
              </a>
            )}
            <span className="text-ink-muted">wysłane {dateTime(q.createdAt)}</span>
          </div>
        </div>
        <Badge tone={q.consentMarketing ? "good" : "neutral"}>
          {q.consentMarketing ? "Zgoda na oferty" : "Bez zgody marketingowej"}
        </Badge>
      </header>

      <div className="mt-3 rounded-md border border-line p-3">
        {q.offer ? (
          <OfferSummary offer={q.offer} labels={labels} />
        ) : (
          q.criteria && (
            <>
              <RequirementHeadline criteria={q.criteria} labels={labels} />
              <RequirementFacts criteria={q.criteria} labels={labels} />
            </>
          )
        )}
        {q.message && (
          <p className="mt-3 text-[13px] whitespace-pre-line text-ink-secondary">
            „{q.message}”
          </p>
        )}
      </div>

      {isNew && q.possibleDuplicates.length > 0 && (
        <div className="mt-3 rounded-md border border-warning/40 bg-warning/12 px-3 py-2.5">
          <p className="flex items-center gap-1.5 text-[12px] font-medium text-warning-ink">
            <Users className="size-3.5" strokeWidth={2} />
            Ta osoba może już być w bazie klientów
          </p>
          <ul className="mt-2 flex flex-col gap-1.5">
            {q.possibleDuplicates.map((d) => (
              <li key={d.id} className="flex flex-wrap items-center justify-between gap-2 text-[13px]">
                <span className="min-w-0">
                  <Link to={`/klienci/${d.id}`} className="font-medium text-ink hover:text-accent">
                    {d.name}
                  </Link>
                  <span className="ml-2 text-[12px] text-ink-secondary">
                    ten sam {d.matchedBy.join(" i ")}
                  </span>
                </span>
                <Button
                  type="button"
                  size="sm"
                  variant="secondary"
                  disabled={busy}
                  onClick={() => onConvert(d.id)}
                >
                  Dopnij do tego klienta
                </Button>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Zgłoszenie sprzedaży nie ma czego dopasowywać do ofert. */}
      <InquiryMatches inquiryId={q.id} enabled={isNew && q.intent === "BUY"} />
      {isNew && q.intent === "SELL" && (
        <p className="mt-3 text-[12px] text-ink-muted">
          Po przyjęciu opis nieruchomości trafi do notatki klienta. Ofertę dodasz po
          rozmowie albo oględzinach, bo formularz nie zbiera adresu ani zdjęć.
        </p>
      )}

      <footer className="mt-3 flex flex-wrap items-center justify-between gap-2 border-t border-line pt-3">
        {isNew ? (
          <>
            <p className="text-[11px] text-ink-muted" title={q.consentText}>
              Zgoda na przetwarzanie danych: {dateTime(q.consentProcessingAt)}
            </p>
            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={onReject}
                disabled={busy}
                className="hover:bg-critical/8 hover:text-critical"
              >
                <X className="size-4" strokeWidth={2} />
                Odrzuć
              </Button>
              <Button
                type="button"
                size="sm"
                variant={q.possibleDuplicates.length > 0 ? "secondary" : "primary"}
                onClick={() => onConvert()}
                disabled={busy}
              >
                <UserPlus className="size-4" strokeWidth={2} />
                {busy ? "Przyjmowanie…" : "Utwórz klienta"}
              </Button>
            </div>
          </>
        ) : (
          <p className="text-[12px] text-ink-secondary">
            Przyjęte {q.handledAt && dateTime(q.handledAt)}
            {q.handledByName && ` przez ${q.handledByName}`}
            {q.clientId && (
              <>
                {". "}
                <Link to={`/klienci/${q.clientId}`} className="font-medium text-accent hover:underline">
                  {q.clientName}
                </Link>
              </>
            )}
          </p>
        )}
      </footer>
    </article>
  );
}

/** Zgłoszenie sprzedaży: co właściciel chce powierzyć biuru. */
function OfferSummary({ offer, labels }: { offer: SaleOffer; labels: RequirementLabels }) {
  const facts: [string, string | null][] = [
    ["Powierzchnia", offer.area != null ? `${formatNumber(offer.area)} m²` : null],
    ["Pokoje", offer.roomsCount != null ? formatNumber(offer.roomsCount) : null],
    [
      "Oczekiwana cena",
      offer.expectedPrice != null ? formatCurrency(offer.expectedPrice) : "do wyceny",
    ],
  ];

  return (
    <div className="min-w-0">
      <div className="flex flex-wrap items-center gap-2">
        <Badge tone="accent">Sprzedaż</Badge>
        <h3 className="text-[13px] font-semibold tracking-tight text-ink">
          {labels.propertyType.get(offer.propertyType) ?? offer.propertyType}
        </h3>
      </div>
      <p className="mt-1 flex items-start gap-1 text-[12px] text-ink-secondary">
        <MapPin className="mt-px size-3.5 shrink-0 text-ink-muted" strokeWidth={2} />
        {offer.district ? `${offer.city}. ${offer.district}` : offer.city}
      </p>
      <dl className="mt-3 grid grid-cols-2 gap-x-6 gap-y-2 text-[13px] sm:grid-cols-4">
        {facts
          .filter((fact): fact is [string, string] => Boolean(fact[1]))
          .map(([name, value]) => (
            <div key={name} className="flex flex-col gap-0.5">
              <dt className="text-[11px] font-medium tracking-wide text-ink-muted uppercase">
                {name}
              </dt>
              <dd className="tabular-nums text-ink">{value}</dd>
            </div>
          ))}
      </dl>
    </div>
  );
}

/** Pasujące oferty jeszcze przed przyjęciem. Agent dzwoni od razu z konkretami. */
function InquiryMatches({ inquiryId, enabled }: { inquiryId: string; enabled: boolean }) {
  const [matches, setMatches] = useState<PropertyMatch[] | null>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!enabled) return;
    fetchInquiryMatches(inquiryId).then(setMatches).catch(() => setMatches([]));
  }, [inquiryId, enabled]);

  if (!enabled || matches === null) return null;
  if (matches.length === 0) {
    return <p className="mt-3 text-[12px] text-ink-muted">Brak pasujących ofert w biurze.</p>;
  }

  return (
    <div className="mt-3">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        className="flex items-center gap-1.5 text-[12px] font-medium text-accent hover:text-accent-hover"
      >
        <ChevronDown className={cn("size-3.5 transition-transform", open && "rotate-180")} strokeWidth={2.25} />
        Pasujące oferty: {matches.length}
      </button>
      {open && (
        <ul className="mt-2 flex flex-col gap-2">
          {matches.map(({ property: p, criteria }) => (
            <li key={p.id} className="rounded-md border border-line px-3 py-2.5">
              <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
                <span className="flex min-w-0 flex-wrap items-baseline gap-x-2">
                  <span className="font-mono text-[11px] text-ink-muted">{p.referenceNumber}</span>
                  <Link to={`/nieruchomosci/${p.id}`} className="text-[13px] font-medium text-ink hover:text-accent">
                    {p.title}
                  </Link>
                </span>
                <span className="text-[13px] font-medium tabular-nums text-ink">
                  {formatCurrency(p.price)}
                  <span className="ml-1 text-[12px] font-normal text-ink-muted">
                    · {formatNumber(p.totalArea)} m²
                  </span>
                </span>
              </div>
              <p className="mt-0.5 mb-2 text-[12px] text-ink-secondary">
                {p.city}
                {p.district && `, ${p.district}`}
              </p>
              <MatchCriteria criteria={criteria} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
