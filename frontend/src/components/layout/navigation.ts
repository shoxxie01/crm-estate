import {
  Building2,
  CalendarDays,
  FileSignature,
  Inbox,
  LayoutDashboard,
  Share2,
  SquareKanban,
  Users,
  type LucideIcon,
} from "lucide-react";

export interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  badge?: string;
}

export const primaryNav: NavItem[] = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/kalendarz", label: "Kalendarz", icon: CalendarDays },
  { to: "/kanban", label: "Kanban", icon: SquareKanban },
  { to: "/nieruchomosci", label: "Nieruchomości", icon: Building2 },
  { to: "/klienci", label: "Klienci", icon: Users },
  // Licznik nowych zgłoszeń dokłada Sidebar. Jest żywy, a nie stały.
  { to: "/zgloszenia", label: "Zgłoszenia", icon: Inbox },
  { to: "/umowy", label: "Umowy", icon: FileSignature, badge: "2" },
  { to: "/eksport", label: "Eksport na portale", icon: Share2 },
];
