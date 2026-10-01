import { useEffect, useId, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowRight, Building2, Search, User } from "lucide-react";
import {
  SEARCH_MIN_LENGTH,
  quickSearch,
  type SearchResults,
} from "../../api/search";
import { formatCurrency } from "../../lib/format";
import { cn } from "../../lib/cn";

/** Jeden wiersz podpowiedzi. Płaska lista, żeby strzałki szły przez grupy. */
interface Option {
  key: string;
  group: "clients" | "properties" | "more";
  to: string;
  title: string;
  subtitle?: string;
}

function toOptions(query: string, results: SearchResults): Option[] {
  const options: Option[] = [];

  for (const client of results.clients) {
    options.push({
      key: `c-${client.id}`,
      group: "clients",
      to: `/klienci/${client.id}`,
      title: `${client.firstName} ${client.lastName}`,
      subtitle: [client.phone, client.email].filter(Boolean).join(" · "),
    });
  }

  for (const property of results.properties) {
    const place = [property.city, property.district, property.street]
      .filter(Boolean)
      .join(", ");
    options.push({
      key: `p-${property.id}`,
      group: "properties",
      to: `/nieruchomosci/${property.id}`,
      title: property.title || property.referenceNumber || "Oferta bez tytułu",
      subtitle: [
        property.referenceNumber,
        place,
        property.price != null ? formatCurrency(property.price) : null,
      ]
        .filter(Boolean)
        .join(" · "),
    });
  }

  // Pełna lista ma sens tylko wtedy, gdy podpowiedzi czegoś nie pokazały.
  if (results.clientsTotal > results.clients.length) {
    options.push({
      key: "more-clients",
      group: "more",
      to: `/klienci?szukaj=${encodeURIComponent(query)}`,
      title: `Wszyscy klienci pasujący do „${query}” (${results.clientsTotal})`,
    });
  }

  return options;
}

const GROUP_LABELS: Record<Option["group"], string | null> = {
  clients: "Klienci",
  properties: "Oferty",
  more: null,
};

export function GlobalSearch() {
  const navigate = useNavigate();
  const listId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const rootRef = useRef<HTMLDivElement>(null);

  const [query, setQuery] = useState("");
  const [options, setOptions] = useState<Option[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);

  const term = query.trim();
  const searchable = term.length >= SEARCH_MIN_LENGTH;

  // Ctrl/Cmd + K przenosi kursor do wyszukiwarki z każdego miejsca aplikacji.
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        inputRef.current?.focus();
        inputRef.current?.select();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  // Odpytujemy po chwili od ostatniego znaku. Flaga `stale` odrzuca odpowiedź
  // na starszą frazę, która przyszła po nowszej.
  useEffect(() => {
    if (!searchable) {
      setOptions([]);
      setLoading(false);
      return;
    }

    let stale = false;
    setLoading(true);
    const timer = setTimeout(() => {
      quickSearch(term)
        .then((results) => {
          if (stale) return;
          setOptions(toOptions(term, results));
          setActive(0);
          setError(false);
        })
        .catch(() => {
          if (!stale) setError(true);
        })
        .finally(() => {
          if (!stale) setLoading(false);
        });
    }, 250);

    return () => {
      stale = true;
      clearTimeout(timer);
    };
  }, [term, searchable]);

  // Lista zamyka się po kliknięciu poza wyszukiwarką.
  useEffect(() => {
    if (!open) return;
    const onPointerDown = (event: PointerEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("pointerdown", onPointerDown);
    return () => document.removeEventListener("pointerdown", onPointerDown);
  }, [open]);

  const choose = (option: Option) => {
    navigate(option.to);
    setQuery("");
    setOpen(false);
    inputRef.current?.blur();
  };

  const onKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "Escape") {
      if (open && query) {
        setOpen(false);
      } else {
        setQuery("");
        inputRef.current?.blur();
      }
      return;
    }

    if (!options.length) return;

    if (event.key === "ArrowDown") {
      event.preventDefault();
      setOpen(true);
      setActive((index) => (index + 1) % options.length);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setOpen(true);
      setActive((index) => (index - 1 + options.length) % options.length);
    } else if (event.key === "Enter" && open) {
      event.preventDefault();
      const option = options[active];
      if (option) choose(option);
    }
  };

  const showPanel = open && searchable;
  const activeId =
    showPanel && options[active] ? `${listId}-${options[active].key}` : undefined;

  return (
    <div ref={rootRef} className="relative max-w-md flex-1">
      <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-ink-muted" />
      <input
        ref={inputRef}
        type="search"
        role="combobox"
        aria-label="Szukaj klientów i ofert"
        aria-expanded={showPanel}
        aria-controls={listId}
        aria-activedescendant={activeId}
        aria-autocomplete="list"
        autoComplete="off"
        placeholder="Szukaj klientów i ofert…"
        value={query}
        onChange={(event) => {
          setQuery(event.target.value);
          setOpen(true);
        }}
        onFocus={() => setOpen(true)}
        onKeyDown={onKeyDown}
        className="h-9 w-full rounded-md border border-line bg-canvas pr-14 pl-8 text-sm text-ink transition-colors placeholder:text-ink-muted hover:border-line-strong focus:border-accent focus:bg-surface focus:ring-2 focus:ring-accent-ring/50 focus:outline-none"
      />
      <kbd className="pointer-events-none absolute top-1/2 right-2 hidden -translate-y-1/2 rounded border border-line bg-surface px-1.5 py-px text-[11px] text-ink-muted sm:block">
        Ctrl K
      </kbd>

      {showPanel && (
        <div className="card absolute inset-x-0 top-full z-30 mt-1.5 max-h-[70vh] overflow-y-auto p-1 shadow-popover">
          {options.length === 0 && (
            <p className="px-2.5 py-3 text-[13px] text-ink-muted">
              {error
                ? "Nie udało się wyszukać. Spróbuj ponownie."
                : loading
                  ? "Szukam…"
                  : `Nic nie pasuje do „${term}”.`}
            </p>
          )}

          <ul id={listId} role="listbox" aria-label="Wyniki wyszukiwania">
            {options.map((option, index) => {
              const label =
                index === 0 || options[index - 1].group !== option.group
                  ? GROUP_LABELS[option.group]
                  : null;
              const Icon =
                option.group === "clients"
                  ? User
                  : option.group === "properties"
                    ? Building2
                    : ArrowRight;

              return (
                <li key={option.key} role="presentation">
                  {label && (
                    <p
                      role="presentation"
                      className="px-2.5 pt-2 pb-1 text-[11px] font-medium tracking-wide text-ink-muted uppercase"
                    >
                      {label}
                    </p>
                  )}
                  <div
                    id={`${listId}-${option.key}`}
                    role="option"
                    aria-selected={index === active}
                    // mousedown zamiast click: input nie traci fokusu przed nawigacją.
                    onMouseDown={(event) => {
                      event.preventDefault();
                      choose(option);
                    }}
                    onMouseEnter={() => setActive(index)}
                    className={cn(
                      "flex cursor-pointer items-center gap-2.5 rounded px-2.5 py-1.5",
                      index === active && "bg-subtle",
                      option.group === "more" && "mt-1 border-t border-line pt-2",
                    )}
                  >
                    <Icon
                      className="size-4 shrink-0 text-ink-muted"
                      strokeWidth={1.75}
                    />
                    <span className="min-w-0">
                      <span
                        className={cn(
                          "block truncate text-[13px]",
                          option.group === "more"
                            ? "text-accent"
                            : "font-medium text-ink",
                        )}
                      >
                        {option.title}
                      </span>
                      {option.subtitle && (
                        <span className="block truncate text-[12px] text-ink-muted tabular-nums">
                          {option.subtitle}
                        </span>
                      )}
                    </span>
                  </div>
                </li>
              );
            })}
          </ul>
        </div>
      )}
    </div>
  );
}
