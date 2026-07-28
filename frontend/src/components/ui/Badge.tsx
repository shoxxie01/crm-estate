import type { ReactNode } from "react";
import { cn } from "../../lib/cn";

type Tone = "neutral" | "accent" | "good" | "warning" | "critical";

const tones: Record<Tone, string> = {
  neutral: "bg-subtle text-ink-secondary border-line",
  accent: "bg-accent-subtle text-accent border-accent-ring/60",
  good: "bg-good/8 text-[#0a7a0a] border-good/30",
  warning: "bg-warning/12 text-[#8a5c00] border-warning/40",
  critical: "bg-critical/8 text-critical border-critical/30",
};

export function Badge({
  tone = "neutral",
  icon,
  children,
}: {
  tone?: Tone;
  icon?: ReactNode;
  children: ReactNode;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded border px-1.5 py-0.5 text-[11px] font-medium whitespace-nowrap",
        tones[tone],
      )}
    >
      {icon}
      {children}
    </span>
  );
}
