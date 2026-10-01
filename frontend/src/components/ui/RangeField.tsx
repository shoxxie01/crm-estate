import type { ChangeEvent } from "react";
import { cn } from "../../lib/cn";

/** Klasy pola tekstowego zgodne z `Input`. Dla pól składanych ręcznie. */
export function fieldClass(invalid: boolean) {
  return cn(
    "h-9 w-full min-w-0 rounded-md border bg-surface px-3 text-sm text-ink tabular-nums transition-colors",
    "placeholder:text-ink-muted focus:outline-none focus-visible:outline-none",
    invalid
      ? "border-critical focus:border-critical focus:ring-2 focus:ring-critical/20"
      : "border-line hover:border-line-strong focus:border-accent focus:ring-2 focus:ring-accent-ring/50",
  );
}

/** Dwa pola „od – do" pod jedną etykietą. Każda granica opcjonalna. */
export function RangeField({
  label,
  hint,
  min,
  max,
  onMin,
  onMax,
  inputMode,
  error,
}: {
  label: string;
  hint?: string;
  min: string;
  max: string;
  onMin: (event: ChangeEvent<HTMLInputElement>) => void;
  onMax: (event: ChangeEvent<HTMLInputElement>) => void;
  inputMode: "numeric" | "decimal";
  error?: string;
}) {
  return (
    <fieldset className="flex flex-col gap-1.5">
      <legend className="mb-1.5 text-[13px] font-medium text-ink">{label}</legend>
      <div className="flex items-center gap-1.5">
        <input
          aria-label={`${label} od`}
          placeholder="od"
          inputMode={inputMode}
          value={min}
          onChange={onMin}
          className={fieldClass(false)}
        />
        <span className="text-ink-muted" aria-hidden="true">
          –
        </span>
        <input
          aria-label={`${label} do`}
          placeholder="do"
          inputMode={inputMode}
          value={max}
          onChange={onMax}
          aria-invalid={error ? true : undefined}
          className={fieldClass(Boolean(error))}
        />
      </div>
      {error ? (
        <p className="text-[12px] text-critical">{error}</p>
      ) : hint ? (
        <p className="text-[12px] text-ink-muted">{hint}</p>
      ) : null}
    </fieldset>
  );
}
