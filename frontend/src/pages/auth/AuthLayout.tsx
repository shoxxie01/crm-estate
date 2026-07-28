import type { ReactNode } from "react";
import { Building2, CalendarDays, Share2 } from "lucide-react";
import { Logo } from "../../components/ui/Logo";

const highlights = [
  {
    icon: Building2,
    title: "Nieruchomości i klienci w jednym miejscu",
    body: "Kartoteka ofert, predyspozycje zakupowe klientów i automatyczne dopasowania.",
  },
  {
    icon: Share2,
    title: "Eksport na portale jednym kliknięciem",
    body: "Otodom, OLX, Gratka i Nieruchomosci-online — z kontrolą statusu synchronizacji.",
  },
  {
    icon: CalendarDays,
    title: "Kalendarz prezentacji i umów",
    body: "Terminy, przypomnienia i historia kontaktu przypięte do konkretnej oferty.",
  },
];

export function AuthLayout({
  title,
  subtitle,
  children,
  footer,
}: {
  title: string;
  subtitle: string;
  children: ReactNode;
  footer: ReactNode;
}) {
  return (
    <div className="grid min-h-dvh lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
      {/* Formularz */}
      <div className="flex flex-col bg-surface px-6 py-8 sm:px-10">
        <Logo />

        <div className="flex flex-1 items-center justify-center py-10">
          <div className="w-full max-w-[360px]">
            <h1 className="text-xl font-semibold tracking-tight text-ink">
              {title}
            </h1>
            <p className="mt-1 text-[13px] text-ink-secondary">{subtitle}</p>

            <div className="mt-7">{children}</div>

            <div className="mt-6 text-[13px] text-ink-secondary">{footer}</div>
          </div>
        </div>

        <p className="text-[12px] text-ink-muted">
          © {new Date().getFullYear()} Delta CRM
        </p>
      </div>

      {/* Panel marki — wyciszony, bez gradientów i cieni */}
      <aside className="hidden border-l border-line bg-canvas px-12 py-16 lg:flex lg:flex-col lg:justify-center">
        <div className="max-w-[420px]">
          <h2 className="text-[22px] leading-snug font-semibold tracking-tight text-ink">
            CRM zbudowany pod codzienną pracę biura nieruchomości.
          </h2>
          <p className="mt-2.5 text-[13px] leading-relaxed text-ink-secondary">
            Bez przeklikiwania się przez pięć narzędzi. Wszystko, od pierwszego
            kontaktu po podpisaną umowę, w jednym widoku.
          </p>

          <ul className="mt-9 flex flex-col gap-6">
            {highlights.map(({ icon: Icon, title: heading, body }) => (
              <li key={heading} className="flex gap-3">
                <span className="mt-0.5 grid size-8 shrink-0 place-items-center rounded-md border border-line bg-surface">
                  <Icon
                    className="size-4 text-accent"
                    strokeWidth={1.75}
                    aria-hidden="true"
                  />
                </span>
                <div>
                  <p className="text-[13px] font-medium text-ink">{heading}</p>
                  <p className="mt-0.5 text-[12px] leading-relaxed text-ink-secondary">
                    {body}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </aside>
    </div>
  );
}
