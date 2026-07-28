import type { ReactNode } from "react";
import { cn } from "../../lib/cn";

interface CardProps {
  title?: string;
  description?: string;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
  bodyClassName?: string;
}

export function Card({
  title,
  description,
  action,
  children,
  className,
  bodyClassName,
}: CardProps) {
  return (
    <section className={cn("card flex flex-col", className)}>
      {(title || action) && (
        <header className="flex items-start justify-between gap-4 border-b border-line px-4 py-3">
          <div className="min-w-0">
            {title && (
              <h2 className="text-[13px] font-semibold tracking-tight text-ink">
                {title}
              </h2>
            )}
            {description && (
              <p className="mt-0.5 text-[12px] text-ink-muted">{description}</p>
            )}
          </div>
          {action && <div className="shrink-0">{action}</div>}
        </header>
      )}
      <div className={cn("flex-1 p-4", bodyClassName)}>{children}</div>
    </section>
  );
}
