import {
  Building2,
  CalendarDays,
  FileSignature,
  LayoutDashboard,
  Share2,
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
  { to: "/kalendarz", label: "Kalendarz", icon: CalendarDays, badge: "4" },
  { to: "/nieruchomosci", label: "Nieruchomości", icon: Building2 },
  { to: "/klienci", label: "Klienci", icon: Users },
  { to: "/umowy", label: "Umowy", icon: FileSignature, badge: "2" },
  { to: "/eksport", label: "Eksport na portale", icon: Share2 },
];
