import { useEffect, useRef, useState } from "react";
import { Bell, Menu, Plus, Search } from "lucide-react";
import { useAuth } from "../../auth/AuthContext";
import { Button } from "../ui/Button";
import { cn } from "../../lib/cn";

function initials(firstName: string, lastName: string) {
  return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
}

export function Topbar({ onOpenMenu }: { onOpenMenu: () => void }) {
  const { user, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const searchRef = useRef<HTMLInputElement>(null);

  // Globalne wyszukiwanie: Ctrl/Cmd + K
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        searchRef.current?.focus();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  return (
    <header className="sticky top-0 z-30 flex h-14 items-center gap-3 border-b border-line bg-surface/90 px-4 backdrop-blur-sm">
      <button
        onClick={onOpenMenu}
        aria-label="Otwórz menu"
        className="rounded p-1.5 text-ink-secondary hover:bg-subtle lg:hidden"
      >
        <Menu className="size-5" strokeWidth={1.75} />
      </button>

      <div className="relative max-w-md flex-1">
        <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-ink-muted" />
        <input
          ref={searchRef}
          type="search"
          placeholder="Szukaj ofert, klientów, umów…"
          aria-label="Szukaj"
          className="h-9 w-full rounded-md border border-line bg-canvas pr-14 pl-8 text-sm text-ink transition-colors placeholder:text-ink-muted hover:border-line-strong focus:border-accent focus:bg-surface focus:ring-2 focus:ring-accent-ring/50 focus:outline-none"
        />
        <kbd className="pointer-events-none absolute top-1/2 right-2 hidden -translate-y-1/2 rounded border border-line bg-surface px-1.5 py-px text-[11px] text-ink-muted sm:block">
          Ctrl K
        </kbd>
      </div>

      <div className="ml-auto flex items-center gap-2">
        <Button size="sm" className="hidden sm:inline-flex">
          <Plus className="size-4" strokeWidth={2} />
          Dodaj ofertę
        </Button>

        <button
          aria-label="Powiadomienia (3 nieprzeczytane)"
          className="relative rounded p-1.5 text-ink-secondary hover:bg-subtle hover:text-ink"
        >
          <Bell className="size-[18px]" strokeWidth={1.75} />
          <span className="absolute top-1 right-1 size-1.5 rounded-full bg-critical ring-2 ring-surface" />
        </button>

        <div className="relative">
          <button
            onClick={() => setMenuOpen((open) => !open)}
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            className={cn(
              "flex items-center gap-2 rounded-md py-1 pr-2 pl-1 hover:bg-subtle",
              menuOpen && "bg-subtle",
            )}
          >
            <span className="grid size-7 place-items-center rounded-full bg-accent-subtle text-[11px] font-semibold text-accent">
              {user ? initials(user.firstName, user.lastName) : "–"}
            </span>
            <span className="hidden text-[13px] font-medium text-ink md:block">
              {user?.firstName} {user?.lastName}
            </span>
          </button>

          {menuOpen && (
            <>
              <div
                className="fixed inset-0 z-10"
                onClick={() => setMenuOpen(false)}
                aria-hidden="true"
              />
              <div
                role="menu"
                className="card absolute right-0 z-20 mt-1.5 w-56 p-1 shadow-[0_4px_16px_rgba(26,29,33,0.08)]"
              >
                <div className="border-b border-line px-2.5 py-2">
                  <p className="truncate text-[13px] font-medium text-ink">
                    {user?.firstName} {user?.lastName}
                  </p>
                  <p className="truncate text-[12px] text-ink-muted">
                    {user?.email}
                  </p>
                </div>
                <button
                  role="menuitem"
                  onClick={logout}
                  className="mt-1 w-full rounded px-2.5 py-1.5 text-left text-[13px] text-ink-secondary hover:bg-subtle hover:text-ink"
                >
                  Wyloguj się
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
