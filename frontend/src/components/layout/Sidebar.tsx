import { useEffect, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { INQUIRIES_CHANGED, fetchNewInquiryCount } from "../../api/inquiries";
import { LifeBuoy, Settings, X } from "lucide-react";
import { Logo } from "../ui/Logo";
import { primaryNav } from "./navigation";
import { cn } from "../../lib/cn";

const linkBase =
  "group flex items-center gap-2.5 rounded-md px-2.5 py-[7px] text-[13px] transition-colors";

/**
 * Liczba nowych zgłoszeń z formularza. Odświeżana przy zmianie strony, po
 * zmianie w skrzynce i co minutę — zgłoszenia przychodzą z zewnątrz, więc
 * inaczej agent nie zobaczyłby nowego, dopóki sam nie wejdzie do skrzynki.
 */
function useNewInquiryCount() {
  const { pathname } = useLocation();
  const [count, setCount] = useState(0);

  useEffect(() => {
    let cancelled = false;
    const refresh = () =>
      fetchNewInquiryCount()
        .then((r) => !cancelled && setCount(r.new))
        .catch(() => undefined);

    refresh();
    const timer = setInterval(refresh, 60_000);
    window.addEventListener(INQUIRIES_CHANGED, refresh);
    return () => {
      cancelled = true;
      clearInterval(timer);
      window.removeEventListener(INQUIRIES_CHANGED, refresh);
    };
  }, [pathname]);

  return count;
}

function NavItems({ onNavigate }: { onNavigate?: () => void }) {
  const newInquiries = useNewInquiryCount();

  return (
    <nav className="flex flex-col gap-0.5">
      {primaryNav.map(({ to, label, icon: Icon, badge: staticBadge }) => {
        const badge =
          to === "/zgloszenia" ? (newInquiries > 0 ? String(newInquiries) : undefined) : staticBadge;
        return (
        <NavLink
          key={to}
          to={to}
          end={to === "/"}
          onClick={onNavigate}
          className={({ isActive }) =>
            cn(
              linkBase,
              isActive
                ? "bg-accent-subtle font-medium text-accent"
                : "text-ink-secondary hover:bg-subtle hover:text-ink",
            )
          }
        >
          {({ isActive }) => (
            <>
              <Icon
                className={cn(
                  "size-4 shrink-0",
                  isActive ? "text-accent" : "text-ink-muted",
                )}
                strokeWidth={1.75}
              />
              <span className="truncate">{label}</span>
              {badge && (
                <span
                  className={cn(
                    "ml-auto rounded px-1.5 py-px text-[11px] font-medium tabular-nums ring-1",
                    to === "/zgloszenia"
                      ? "bg-accent text-white ring-accent"
                      : "bg-surface text-ink-secondary ring-line",
                  )}
                >
                  {badge}
                </span>
              )}
            </>
          )}
        </NavLink>
        );
      })}
    </nav>
  );
}

export function SidebarContent({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <div className="flex h-full flex-col">
      <div className="flex h-14 items-center px-4">
        <Logo />
      </div>

      <div className="flex-1 overflow-y-auto px-3 pb-4">
        <p className="px-2.5 pt-2 pb-1.5 text-[11px] font-medium tracking-wide text-ink-muted uppercase">
          Moduły
        </p>
        <NavItems onNavigate={onNavigate} />
      </div>

      <div className="border-t border-line px-3 py-3">
        <nav className="flex flex-col gap-0.5">
          <a href="#ustawienia" className={cn(linkBase, "text-ink-secondary hover:bg-subtle hover:text-ink")}>
            <Settings className="size-4 text-ink-muted" strokeWidth={1.75} />
            Ustawienia
          </a>
          <a href="#pomoc" className={cn(linkBase, "text-ink-secondary hover:bg-subtle hover:text-ink")}>
            <LifeBuoy className="size-4 text-ink-muted" strokeWidth={1.75} />
            Pomoc
          </a>
        </nav>
      </div>
    </div>
  );
}

export function Sidebar({
  mobileOpen,
  onClose,
}: {
  mobileOpen: boolean;
  onClose: () => void;
}) {
  return (
    <>
      {/* Desktop — stały panel */}
      <aside className="fixed inset-y-0 left-0 hidden w-60 border-r border-line bg-surface lg:block">
        <SidebarContent />
      </aside>

      {/* Mobile — szuflada */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div
            className="absolute inset-0 bg-ink/25"
            onClick={onClose}
            aria-hidden="true"
          />
          <aside className="absolute inset-y-0 left-0 w-64 border-r border-line bg-surface">
            <button
              onClick={onClose}
              aria-label="Zamknij menu"
              className="absolute top-3.5 right-3 rounded p-1 text-ink-muted hover:bg-subtle hover:text-ink"
            >
              <X className="size-4" />
            </button>
            <SidebarContent onNavigate={onClose} />
          </aside>
        </div>
      )}
    </>
  );
}
