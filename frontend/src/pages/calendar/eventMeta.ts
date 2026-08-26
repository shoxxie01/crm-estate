import {
  Camera,
  CheckCircle2,
  ClipboardList,
  DoorOpen,
  FileSignature,
  Home,
  Phone,
  Ruler,
  UserRound,
  type LucideIcon,
} from "lucide-react";

/**
 * Ikony, kolory i tonacje terminów.
 *
 * Kafelek w siatce koduje dwie rzeczy naraz, więc mają rozdzielone kanały:
 * **rodzaj niesie kolor** (tło, obramowanie, ikona, kropka), a **status —
 * sposób podania** (pogrubienie, wyblaknięcie, przekreślenie, obwódka przy
 * nieobecności). Gdyby oba sięgały po kolor, jedno musiałoby ustąpić.
 *
 * Kolor nie jest jedynym nośnikiem rodzaju: obok zawsze stoi ikona, a w panelu
 * i na osi czasu również etykieta słownikowa. To istotne, bo dziewięciu
 * kategorii nie da się rozdzielić samym odcieniem przy daltonizmie — paleta
 * rozstrzela więc także jasność (L* od 29 do 56), co przy protanopii
 * i deuteranopii robi całą robotę. Liczby w `frontend/README.md`.
 */

type TypeMeta = {
  icon: LucideIcon;
  /** Kafelek w siatce: tinta + obramowanie. */
  chip: string;
  /** Kropka legendy i znacznik na osi czasu. */
  dot: string;
  /** Ikona i akcenty tekstowe. */
  ink: string;
};

const types: Record<string, TypeMeta> = {
  PRESENTATION: {
    icon: Home,
    chip: "border-event-presentation/45 bg-event-presentation-tint",
    dot: "bg-event-presentation",
    ink: "text-event-presentation",
  },
  MEETING: {
    icon: UserRound,
    chip: "border-event-meeting/45 bg-event-meeting-tint",
    dot: "bg-event-meeting",
    ink: "text-event-meeting",
  },
  VALUATION: {
    icon: Ruler,
    chip: "border-event-valuation/45 bg-event-valuation-tint",
    dot: "bg-event-valuation",
    ink: "text-event-valuation",
  },
  CONTRACT_SIGNING: {
    icon: FileSignature,
    chip: "border-event-contract/45 bg-event-contract-tint",
    dot: "bg-event-contract",
    ink: "text-event-contract",
  },
  OPEN_HOUSE: {
    icon: DoorOpen,
    chip: "border-event-open-house/45 bg-event-open-house-tint",
    dot: "bg-event-open-house",
    ink: "text-event-open-house",
  },
  PHOTO_SESSION: {
    icon: Camera,
    chip: "border-event-photo/45 bg-event-photo-tint",
    dot: "bg-event-photo",
    ink: "text-event-photo",
  },
  PHONE_CALL: {
    icon: Phone,
    chip: "border-event-phone/45 bg-event-phone-tint",
    dot: "bg-event-phone",
    ink: "text-event-phone",
  },
  TASK: {
    icon: ClipboardList,
    chip: "border-event-task/45 bg-event-task-tint",
    dot: "bg-event-task",
    ink: "text-event-task",
  },
  OTHER: {
    icon: CheckCircle2,
    chip: "border-event-other/45 bg-event-other-tint",
    dot: "bg-event-other",
    ink: "text-event-other",
  },
};

/** Rodzaj spoza słownika (starszy wpis, nowa wartość w backendzie) leci jako „Inne". */
const fallback = types.OTHER;

export function eventIcon(type: string): LucideIcon {
  return (types[type] ?? fallback).icon;
}

export function typeChip(type: string): string {
  return (types[type] ?? fallback).chip;
}

export function typeDot(type: string): string {
  return (types[type] ?? fallback).dot;
}

export function typeInk(type: string): string {
  return (types[type] ?? fallback).ink;
}

/**
 * Status kafelka — bez własnego koloru poza nieobecnością, która jako jedyna
 * jest problemem wymagającym reakcji i dostaje czerwoną obwódkę ze skali
 * statusów.
 */
export function statusChip(status: string): string {
  switch (status) {
    case "CONFIRMED":
      return "font-medium";
    case "COMPLETED":
      return "opacity-70";
    case "CANCELLED":
      return "opacity-60 line-through";
    case "NO_SHOW":
      return "ring-1 ring-critical/60";
    default:
      return "";
  }
}

export type StatusTone = "open" | "confirmed" | "done" | "cancelled" | "noshow";

export function statusTone(status: string): StatusTone {
  switch (status) {
    case "CONFIRMED":
      return "confirmed";
    case "COMPLETED":
      return "done";
    case "CANCELLED":
      return "cancelled";
    case "NO_SHOW":
      return "noshow";
    default:
      return "open";
  }
}

/** Tonacja odznaki statusu — zgodna z paletą statusów systemu. */
export const badgeTone: Record<
  StatusTone,
  "neutral" | "accent" | "good" | "warning" | "critical"
> = {
  open: "neutral",
  confirmed: "accent",
  done: "good",
  cancelled: "neutral",
  noshow: "critical",
};
