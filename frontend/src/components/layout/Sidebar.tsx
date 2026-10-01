import { useEffect, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { INQUIRIES_CHANGED, fetchNewInquiryCount } from "../../api/inquiries";
import { LifeBuoy, Menu, Settings, X } from "lucide-react";
import { Logo } from "../ui/Logo";
import { primaryNav } from "./navigation";
import { cn } from "../../lib/cn";

const linkBase =
  "group flex items-center gap-2.5 rounded-md px-2.5 py-[7px] text-[13px] transition-colors";

/**
 * Liczba nowych zgłoszeń z formularza. Odświeżana przy zmianie strony, po
 * zmianie w skrzynce i co minutę. Zgłoszenia przychodzą z zewnątrz, więc
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

function NavItems({
  onNavigate,
  collapsed = false,
}: {
  onNavigate?: () => void;
  collapsed?: boolean;
}) {
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
          title={collapsed ? label : undefined}
          aria-label={collapsed ? label : undefined}
          className={({ isActive }) =>
            cn(
              linkBase,
              collapsed && "relative justify-center px-0",
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
              {/* Zwinięty pasek: zamiast plakietki kropka na ikonie. Liczba
                  i tak by się nie zmieściła, a sygnał „coś nowego" zostaje. */}
              {collapsed ? (
                badge &&
                to === "/zgloszenia" && (
                  <span
                    className="absolute top-1 right-2.5 size-2 rounded-full bg-accent ring-2 ring-surface"
                    aria-hidden="true"
                  />
                )
              ) : (
                <span className="truncate">{label}</span>
              )}
              {!collapsed && badge && (
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

export function SidebarContent({
  onNavigate,
  collapsed = false,
  onToggleCollapsed,
}: {
  onNavigate?: () => void;
  collapsed?: boolean;
  /** Tylko na desktopie. W szufladzie mobilnej zwijanie nie ma sensu. */
  onToggleCollapsed?: () => void;
}) {
  const secondary = cn(
    linkBase,
    "text-ink-secondary hover:bg-subtle hover:text-ink",
    collapsed && "justify-center px-0",
  );

  return (
    <div className="flex h-full flex-col">
      {/* Przycisk zwijania stoi w wierszu logo, przy prawej krawędzi. Po
          zwinięciu zostaje sam, bo obok niego nie zmieści się już logo. */}
      <div
        className={cn(
          "flex h-14 items-center",
          collapsed ? "justify-center" : "justify-between pr-3 pl-4",
        )}
      >
        {!collapsed && <Logo />}
        {onToggleCollapsed && (
          <button
            type="button"
            onClick={onToggleCollapsed}
            title={collapsed ? "Rozwiń menu" : "Zwiń menu"}
            aria-label={collapsed ? "Rozwiń menu" : "Zwiń menu"}
            aria-expanded={!collapsed}
            className="rounded-md p-1.5 text-ink-secondary transition-colors hover:bg-subtle hover:text-ink"
          >
            <Menu className="size-5" strokeWidth={1.75} />
          </button>
        )}
        {collapsed && !onToggleCollapsed && <Logo showWordmark={false} />}
      </div>

      <div className={cn("flex-1 overflow-y-auto pb-4", collapsed ? "px-2" : "px-3")}>
        {collapsed ? (
          <div className="mx-2 mt-2 mb-2 border-t border-line" />
        ) : (
          <p className="px-2.5 pt-2 pb-1.5 text-[11px] font-medium tracking-wide text-ink-muted uppercase">
            Moduły
          </p>
        )}
        <NavItems onNavigate={onNavigate} collapsed={collapsed} />
      </div>

      <div className={cn("border-t border-line py-3", collapsed ? "px-2" : "px-3")}>
        <nav className="flex flex-col gap-0.5">
          <a href="#ustawienia" className={secondary} title={collapsed ? "Ustawienia" : undefined}>
            <Settings className="size-4 shrink-0 text-ink-muted" strokeWidth={1.75} />
            {!collapsed && "Ustawienia"}
          </a>
          <a href="#pomoc" className={secondary} title={collapsed ? "Pomoc" : undefined}>
            <LifeBuoy className="size-4 shrink-0 text-ink-muted" strokeWidth={1.75} />
            {!collapsed && "Pomoc"}
          </a>
        </nav>
      </div>
    </div>
  );
}

export function Sidebar({
  mobileOpen,
  onClose,
  collapsed,
  onToggleCollapsed,
}: {
  mobileOpen: boolean;
  onClose: () => void;
  collapsed: boolean;
  onToggleCollapsed: () => void;
}) {
  return (
    <>
      {/* Desktop. Stały panel, zwijany do samych ikon */}
      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-20 hidden border-r border-line bg-surface transition-[width] duration-200 lg:block",
          collapsed ? "w-16" : "w-60",
        )}
      >
        <SidebarContent collapsed={collapsed} onToggleCollapsed={onToggleCollapsed} />
      </aside>

      {/* Mobile. Szuflada */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div
            className="absolute inset-0 bg-scrim/25"
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
