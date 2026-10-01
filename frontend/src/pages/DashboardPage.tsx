import { useState } from "react";
import {
  AlertTriangle,
  CheckCircle2,
  ChevronRight,
  XCircle,
} from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { Card } from "../components/ui/Card";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { StatTile } from "../components/charts/StatTile";
import { StackedBarChart } from "../components/charts/StackedBarChart";
import { RankedBarChart } from "../components/charts/RankedBarChart";
import {
  hotLeads,
  kpis,
  leadSources,
  portalSyncs,
  transactionsByMonth,
  upcomingEvents,
  type PortalSync,
} from "../data/dashboard";
import { cn } from "../lib/cn";

const RANGES = ["7 dni", "30 dni", "90 dni", "Ten rok"] as const;

const statusMeta: Record<
  PortalSync["status"],
  { icon: typeof CheckCircle2; color: string; label: string }
> = {
  ok: { icon: CheckCircle2, color: "var(--color-good)", label: "Aktywny" },
  warning: {
    icon: AlertTriangle,
    color: "var(--color-warning)",
    label: "Ostrzeżenie",
  },
  critical: { icon: XCircle, color: "var(--color-critical)", label: "Błąd" },
};

const today = new Intl.DateTimeFormat("pl-PL", {
  weekday: "long",
  day: "numeric",
  month: "long",
}).format(new Date());

export function DashboardPage() {
  const { user } = useAuth();
  const [range, setRange] = useState<(typeof RANGES)[number]>("90 dni");

  return (
    <div className="flex flex-col gap-5">
      {/* Nagłówek + filtry. Jeden rząd nad wykresami */}
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-lg font-semibold tracking-tight text-ink">
            Dzień dobry, {user?.firstName}
          </h1>
          <p className="mt-0.5 text-[13px] text-ink-secondary first-letter:uppercase">
            {today} · {user?.agencyName}
          </p>
        </div>

        <div
          className="flex rounded-md border border-line bg-surface p-0.5"
          role="group"
          aria-label="Zakres dat"
        >
          {RANGES.map((option) => (
            <button
              key={option}
              onClick={() => setRange(option)}
              aria-pressed={range === option}
              className={cn(
                "rounded px-2.5 py-1 text-[12px] transition-colors",
                range === option
                  ? "bg-subtle font-medium text-ink"
                  : "text-ink-secondary hover:text-ink",
              )}
            >
              {option}
            </button>
          ))}
        </div>
      </div>

      {/* KPI */}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {kpis.map((kpi) => (
          <StatTile key={kpi.id} kpi={kpi} />
        ))}
      </div>

      {/* Wykresy */}
      <div className="grid gap-4 lg:grid-cols-3">
        <Card
          className="lg:col-span-2"
          title="Domknięte transakcje"
          description={`Sprzedaż i wynajem, ostatnie 12 miesięcy · zakres: ${range}`}
        >
          <StackedBarChart data={transactionsByMonth} />
        </Card>

        <Card
          title="Źródła pozyskania klientów"
        >
          <RankedBarChart data={leadSources} />
        </Card>
      </div>

      {/* Listy operacyjne */}
      <div className="grid gap-4 lg:grid-cols-3">
        <Card
          title="Najbliższe wydarzenia"
          description="Kalendarz na dziś i jutro"
          action={
            <Button variant="ghost" size="sm">
              Kalendarz
              <ChevronRight className="size-3.5" strokeWidth={2} />
            </Button>
          }
          bodyClassName="p-0"
        >
          <ul className="divide-y divide-line">
            {upcomingEvents.map((event) => (
              <li
                key={event.id}
                className="flex gap-3 px-4 py-2.5 hover:bg-subtle"
              >
                <div className="w-11 shrink-0 pt-0.5">
                  <p className="tabular text-[13px] font-medium text-ink">
                    {event.time}
                  </p>
                  <p className="text-[11px] text-ink-muted">{event.day}</p>
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-[13px] font-medium text-ink">
                    {event.title}
                  </p>
                  <p className="truncate text-[12px] text-ink-secondary">
                    {event.subtitle}
                  </p>
                </div>
                <div className="shrink-0 self-center">
                  <Badge>{event.kind}</Badge>
                </div>
              </li>
            ))}
          </ul>
        </Card>

        <Card
          title="Klienci do kontaktu"
          description="Wg dopasowania predyspozycji do aktywnych ofert"
          bodyClassName="p-0"
        >
          <ul className="divide-y divide-line">
            {hotLeads.map((lead) => (
              <li key={lead.id} className="px-4 py-2.5 hover:bg-subtle">
                <div className="flex items-baseline justify-between gap-2">
                  <p className="truncate text-[13px] font-medium text-ink">
                    {lead.name}
                  </p>
                  <Badge tone="accent">{lead.matches} dopasowań</Badge>
                </div>
                <p className="mt-0.5 truncate text-[12px] text-ink-secondary">
                  {lead.looking} · {lead.budget}
                </p>
                <p
                  className={cn(
                    "mt-0.5 text-[11px]",
                    lead.lastContactDays >= 7
                      ? "text-critical"
                      : "text-ink-muted",
                  )}
                >
                  Ostatni kontakt: {lead.lastContactDays} dni temu
                </p>
              </li>
            ))}
          </ul>
        </Card>

        <Card
          title="Eksport na portale"
          bodyClassName="p-0"
        >
          <ul className="divide-y divide-line">
            {portalSyncs.map((sync) => {
              const meta = statusMeta[sync.status];
              const Icon = meta.icon;
              return (
                <li
                  key={sync.portal}
                  className="flex items-start gap-2.5 px-4 py-2.5 hover:bg-subtle"
                >
                  {/* Kolor statusu zawsze w parze z ikoną i etykietą */}
                  <Icon
                    className="mt-0.5 size-4 shrink-0"
                    style={{ color: meta.color }}
                    strokeWidth={2}
                    aria-hidden="true"
                  />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-baseline justify-between gap-2">
                      <p className="truncate text-[13px] font-medium text-ink">
                        {sync.portal}
                      </p>
                      <span className="tabular shrink-0 text-[12px] text-ink-secondary">
                        {sync.listings} ofert
                      </span>
                    </div>
                    <p className="truncate text-[12px] text-ink-secondary">
                      <span className="sr-only">{meta.label}: </span>
                      {sync.message}
                    </p>
                  </div>
                </li>
              );
            })}
          </ul>
        </Card>
      </div>
    </div>
  );
}
