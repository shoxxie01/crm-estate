import { cn } from "../../lib/cn";

/** Delta — trójkąt (Δ) z wyciętym środkiem, w kolorze akcentu. */
export function LogoMark({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
      className={cn("size-6", className)}
    >
      <path
        d="M12 3.5 21 20H3L12 3.5Z"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinejoin="round"
      />
      <path d="M12 11.5 15.6 18H8.4L12 11.5Z" fill="currentColor" />
    </svg>
  );
}

export function Logo({
  className,
  showWordmark = true,
}: {
  className?: string;
  showWordmark?: boolean;
}) {
  return (
    <span className={cn("inline-flex items-center gap-2", className)}>
      <LogoMark className="size-6 text-accent" />
      {showWordmark && (
        <span className="text-[15px] font-semibold tracking-tight text-ink">
          Delta<span className="text-ink-muted"> CRM</span>
        </span>
      )}
    </span>
  );
}
