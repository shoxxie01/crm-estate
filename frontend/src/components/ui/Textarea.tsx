import { useId, type TextareaHTMLAttributes } from "react";
import { cn } from "../../lib/cn";

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string;
  error?: string;
  hint?: string;
  /** Licznik znaków — przydatny tam, gdzie portal ma twardy limit. */
  counter?: { value: number; max: number };
}

export function Textarea({
  label,
  error,
  hint,
  counter,
  className,
  id,
  required,
  ...props
}: TextareaProps) {
  const generatedId = useId();
  const textareaId = id ?? generatedId;
  const describedBy = error
    ? `${textareaId}-error`
    : hint
      ? `${textareaId}-hint`
      : undefined;

  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-baseline justify-between gap-2">
        <label htmlFor={textareaId} className="text-[13px] font-medium text-ink">
          {label}
          {required && <span className="ml-0.5 text-critical">*</span>}
        </label>
        {counter && (
          <span
            className={cn(
              "text-[11px] tabular-nums",
              counter.value > counter.max ? "text-critical" : "text-ink-muted",
            )}
          >
            {counter.value}/{counter.max}
          </span>
        )}
      </div>

      <textarea
        id={textareaId}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={cn(
          "w-full rounded-md border bg-surface px-3 py-2 text-sm leading-relaxed text-ink transition-colors",
          "placeholder:text-ink-muted",
          "focus:outline-none focus-visible:outline-none",
          error
            ? "border-critical focus:border-critical focus:ring-2 focus:ring-critical/20"
            : "border-line hover:border-line-strong focus:border-accent focus:ring-2 focus:ring-accent-ring/50",
          className,
        )}
        {...props}
      />

      {error ? (
        <p id={`${textareaId}-error`} className="text-[12px] text-critical">
          {error}
        </p>
      ) : hint ? (
        <p id={`${textareaId}-hint`} className="text-[12px] text-ink-muted">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
