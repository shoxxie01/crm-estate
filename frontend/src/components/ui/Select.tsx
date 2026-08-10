import { useId, type SelectHTMLAttributes } from "react";
import { cn } from "../../lib/cn";

export interface Option {
  value: string;
  label: string;
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string;
  options: Option[];
  error?: string;
  hint?: string;
  /** Tekst pustej pozycji. Pomiń dla pól wymaganych bez wartości domyślnej. */
  placeholder?: string;
}

export function Select({
  label,
  options,
  error,
  hint,
  placeholder,
  className,
  id,
  ...props
}: SelectProps) {
  const generatedId = useId();
  const selectId = id ?? generatedId;
  const describedBy = error
    ? `${selectId}-error`
    : hint
      ? `${selectId}-hint`
      : undefined;

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={selectId} className="text-[13px] font-medium text-ink">
        {label}
      </label>

      <select
        id={selectId}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={cn(
          "h-9 w-full rounded-md border bg-surface px-2.5 text-sm text-ink transition-colors",
          "focus:outline-none focus-visible:outline-none",
          error
            ? "border-critical focus:border-critical focus:ring-2 focus:ring-critical/20"
            : "border-line hover:border-line-strong focus:border-accent focus:ring-2 focus:ring-accent-ring/50",
          className,
        )}
        {...props}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>

      {error ? (
        <p id={`${selectId}-error`} className="text-[12px] text-critical">
          {error}
        </p>
      ) : hint ? (
        <p id={`${selectId}-hint`} className="text-[12px] text-ink-muted">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
