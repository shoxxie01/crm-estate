import { Check, type LucideIcon } from "lucide-react";
import { cn } from "../../lib/cn";

export type StepState = "current" | "done" | "error" | "todo";

export interface StepperItem {
  id: string;
  title: string;
  icon: LucideIcon;
  state: StepState;
  /** Czy wolno tu przeskoczyć kliknięciem. Tylko do kroków już odwiedzonych. */
  reachable: boolean;
}

/**
 * Postęp kreatora: licznik „Krok X z N", pasek z odcinkiem na każdy krok
 * i pionowa lista etapów (na szerokim ekranie) w stylu Otodom.
 */
export function PropertyFormStepper({
  steps,
  onSelect,
}: {
  steps: StepperItem[];
  onSelect: (id: string) => void;
}) {
  const currentIndex = steps.findIndex((s) => s.state === "current");
  const current = steps[currentIndex];
  const left = steps.length - currentIndex - 1;
  const percent = Math.round(((currentIndex + 1) / steps.length) * 100);

  return (
    <nav aria-label="Etapy dodawania oferty" className="flex flex-col gap-4">
      <div className="card p-4">
        <div className="flex items-baseline justify-between gap-2">
          <p className="text-[13px] font-semibold text-ink">
            Krok {currentIndex + 1} z {steps.length}
          </p>
          <p className="text-[12px] tabular-nums text-ink-muted">{percent}%</p>
        </div>
        <p className="mt-0.5 text-[12px] text-ink-muted">
          {left === 0
            ? "Ostatni krok. Sprawdź dane i zapisz."
            : `Do końca ${left === 1 ? "został 1 krok" : `zostały ${left} ${pluralSteps(left)}`}.`}
        </p>

        {/* Odcinek na każdy krok. Kolor mówi, co już za nami. */}
        <ol className="mt-3 flex gap-1" aria-hidden="true">
          {steps.map((step) => (
            <li
              key={step.id}
              className={cn(
                "h-1.5 flex-1 rounded-full transition-colors",
                step.state === "current" && "bg-accent",
                step.state === "done" && "bg-accent/45",
                step.state === "error" && "bg-critical",
                step.state === "todo" && "bg-line",
              )}
            />
          ))}
        </ol>

        {/* Na wąskim ekranie lista etapów jest schowana. Wystarczy nazwa bieżącego. */}
        {current && (
          <p className="mt-3 text-[13px] text-ink-secondary lg:hidden">
            {current.title}
          </p>
        )}
      </div>

      <ol className="hidden px-1 lg:flex lg:flex-col">
        {steps.map((step, index) => {
          const last = index === steps.length - 1;
          return (
            <li key={step.id} className="relative flex gap-3 pb-5 last:pb-0">
              {!last && (
                <span
                  aria-hidden="true"
                  className={cn(
                    "absolute top-8 bottom-0 left-[15px] w-0.5 rounded-full",
                    step.state === "done" ? "bg-accent/45" : "bg-line",
                  )}
                />
              )}
              <button
                type="button"
                disabled={!step.reachable || step.state === "current"}
                onClick={() => onSelect(step.id)}
                aria-current={step.state === "current" ? "step" : undefined}
                className="group flex items-center gap-3 text-left disabled:cursor-default"
              >
                <span
                  className={cn(
                    "relative z-10 flex size-8 shrink-0 items-center justify-center rounded-full text-[13px] font-semibold tabular-nums transition-colors",
                    step.state === "current" &&
                      "bg-accent text-white ring-4 ring-accent-ring/60",
                    step.state === "done" && "bg-accent-subtle text-accent",
                    step.state === "error" && "bg-critical text-white",
                    step.state === "todo" && "bg-subtle text-ink-muted",
                  )}
                >
                  {step.state === "done" ? (
                    <Check className="size-4" strokeWidth={2.5} />
                  ) : step.state === "error" ? (
                    "!"
                  ) : (
                    index + 1
                  )}
                </span>
                <span
                  className={cn(
                    "text-[13px] transition-colors",
                    step.state === "current"
                      ? "font-semibold text-ink"
                      : step.state === "error"
                        ? "text-critical"
                        : step.reachable
                          ? "text-ink-secondary group-hover:text-ink"
                          : "text-ink-muted",
                  )}
                >
                  {step.title}
                </span>
              </button>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

// 2–4 → „kroki", reszta (5–21, 25–31…) → „kroków"; 12–14 to zawsze „kroków".
const pluralSteps = (n: number): string => {
  const mod10 = n % 10;
  const mod100 = n % 100;
  return mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)
    ? "kroki"
    : "kroków";
};
