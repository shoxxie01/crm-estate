import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Menu, Moon, Plus, Sun } from "lucide-react";
import { useAuth } from "../../auth/AuthContext";
import { Button } from "../ui/Button";
import { cn } from "../../lib/cn";
import { GlobalSearch } from "./GlobalSearch";
import { useTheme, type Theme } from "../../theme/ThemeContext";

const THEMES: { value: Theme; label: string; icon: typeof Sun }[] = [
  { value: "light", label: "Jasny", icon: Sun },
  { value: "dark", label: "Ciemny", icon: Moon },
];

function initials(firstName: string, lastName: string) {
  return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
}

export function Topbar({ onOpenMenu }: { onOpenMenu: () => void }) {
  const { user, logout } = useAuth();
  const { theme, setTheme } = useTheme();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Menu zamyka każde kliknięcie poza nim i Escape. Nasłuch na dokumencie,
  // a nie zasłona `fixed inset-0`: nagłówek ma backdrop-blur, przez co
  // zasłona pokrywałaby tylko pasek nagłówka, nie cały ekran.
  useEffect(() => {
    if (!menuOpen) return;
    const onPointerDown = (event: PointerEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setMenuOpen(false);
    };
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [menuOpen]);

  return (
    <header className="sticky top-0 z-30 flex h-14 items-center gap-3 border-b border-line bg-surface/90 px-4 backdrop-blur-sm">
      <button
        onClick={onOpenMenu}
        aria-label="Otwórz menu"
        className="rounded p-1.5 text-ink-secondary hover:bg-subtle lg:hidden"
      >
        <Menu className="size-5" strokeWidth={1.75} />
      </button>

      <GlobalSearch />

      <div className="ml-auto flex items-center gap-2">
        <Link to="/nieruchomosci/nowa" className="hidden sm:block">
          <Button size="sm">
            <Plus className="size-4" strokeWidth={2} />
            Dodaj ofertę
          </Button>
        </Link>

        <div ref={menuRef} className="relative">
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
            <div
              role="menu"
              className="card absolute right-0 z-20 mt-1.5 w-56 p-1 shadow-popover"
            >
              <div className="border-b border-line px-2.5 py-2">
                <p className="truncate text-[13px] font-medium text-ink">
                  {user?.firstName} {user?.lastName}
                </p>
                <p className="truncate text-[12px] text-ink-muted">
                  {user?.email}
                </p>
              </div>

              <div className="border-b border-line px-2.5 pt-2 pb-2.5">
                <p className="text-[11px] font-medium tracking-wide text-ink-muted uppercase">
                  Ustawienia
                </p>
                <p
                  id="theme-label"
                  className="mt-1.5 text-[12px] text-ink-secondary"
                >
                  Motyw
                </p>
                <div
                  role="radiogroup"
                  aria-labelledby="theme-label"
                  className="mt-1 grid grid-cols-2 gap-0.5 rounded-md border border-line bg-canvas p-0.5"
                >
                  {THEMES.map(({ value, label, icon: Icon }) => {
                    const active = theme === value;
                    return (
                      <button
                        key={value}
                        role="radio"
                        aria-checked={active}
                        onClick={() => setTheme(value)}
                        className={cn(
                          "flex items-center justify-center gap-1.5 rounded px-2 py-1 text-[12px] transition-colors",
                          active
                            ? "bg-surface font-medium text-ink ring-1 ring-line-strong"
                            : "text-ink-secondary hover:text-ink",
                        )}
                      >
                        <Icon className="size-3.5" strokeWidth={1.75} />
                        {label}
                      </button>
                    );
                  })}
                </div>
              </div>

              <button
                role="menuitem"
                onClick={logout}
                className="mt-1 w-full rounded px-2.5 py-1.5 text-left text-[13px] text-ink-secondary hover:bg-subtle hover:text-ink"
              >
                Wyloguj się
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
