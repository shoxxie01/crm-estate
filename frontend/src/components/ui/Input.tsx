import { useId, type InputHTMLAttributes, type ReactNode } from "react";
import { cn } from "../../lib/cn";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  hint?: string;
  /** Stały, nieedytowalny przedrostek wewnątrz pola (np. kierunkowy +48). */
  leading?: string;
  trailing?: ReactNode;
}

export function Input({
  label,
  error,
  hint,
  leading,
  trailing,
  className,
  id,
  required,
  ...props
}: InputProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const describedBy = error
    ? `${inputId}-error`
    : hint
      ? `${inputId}-hint`
      : undefined;

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={inputId} className="text-[13px] font-medium text-ink">
        {label}
        {required && <span className="ml-0.5 text-critical">*</span>}
      </label>

      <div className="relative">
        {leading && (
          <span
            aria-hidden="true"
            className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-sm tabular-nums text-ink-muted"
          >
            {leading}
          </span>
        )}
        <input
          id={inputId}
          required={required}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          className={cn(
            "h-9 w-full rounded-md border bg-surface px-3 text-sm text-ink transition-colors",
            "placeholder:text-ink-muted",
            "focus:outline-none focus-visible:outline-none",
            error
              ? "border-critical focus:border-critical focus:ring-2 focus:ring-critical/20"
              : "border-line hover:border-line-strong focus:border-accent focus:ring-2 focus:ring-accent-ring/50",
            leading && "pl-12",
            trailing && "pr-10",
            className,
          )}
          {...props}
        />
        {trailing && (
          <div className="absolute inset-y-0 right-0 flex items-center pr-1">
            {trailing}
          </div>
        )}
      </div>

      {error ? (
        <p id={`${inputId}-error`} className="text-[12px] text-critical">
          {error}
        </p>
      ) : hint ? (
        <p id={`${inputId}-hint`} className="text-[12px] text-ink-muted">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
