import { NavLink } from "react-router-dom";
import { cn } from "../../lib/cn";

const TABS = [
  { to: "/kanban", label: "Tablica", end: true },
  { to: "/kanban/raport", label: "Raport", end: false },
];

/** Przełącznik tablica ↔ raport. Ten sam lejek, dwa widoki. */
export function KanbanTabs() {
  return (
    <nav className="flex w-fit rounded-md border border-line bg-surface p-0.5" aria-label="Widok Kanbana">
      {TABS.map((tab) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          end={tab.end}
          className={({ isActive }) =>
            cn(
              "rounded px-2.5 py-1 text-[12px] transition-colors",
              isActive ? "bg-subtle font-medium text-ink" : "text-ink-secondary hover:text-ink",
            )
          }
        >
          {tab.label}
        </NavLink>
      ))}
    </nav>
  );
}
