import { useLocation } from "react-router-dom";
import { primaryNav } from "../components/layout/navigation";

export function PlaceholderPage() {
  const { pathname } = useLocation();
  const item = primaryNav.find((entry) => entry.to === pathname);
  const Icon = item?.icon;

  return (
    <div className="card grid min-h-[420px] place-items-center p-8 text-center">
      <div className="max-w-sm">
        {Icon && (
          <span className="mx-auto mb-4 grid size-10 place-items-center rounded-md border border-line bg-canvas">
            <Icon className="size-5 text-ink-muted" strokeWidth={1.5} />
          </span>
        )}
        <h1 className="text-base font-semibold tracking-tight text-ink">
          {item?.label ?? "Moduł"}
        </h1>
        <p className="mt-1.5 text-[13px] leading-relaxed text-ink-secondary">
          Ten moduł nie jest jeszcze zbudowany. Szata graficzna, nawigacja
          i design tokeny są już na miejscu. Widok wystarczy wypełnić treścią.
        </p>
      </div>
    </div>
  );
}
