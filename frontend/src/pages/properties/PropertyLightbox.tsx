import { useCallback, useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import {
  ChevronLeft,
  ChevronRight,
  Minus,
  Plus,
  RotateCcw,
  X,
} from "lucide-react";
import type { PropertyMedia } from "../../api/properties";

const MIN_SCALE = 1;
const MAX_SCALE = 4;
const STEP = 0.25;

const clamp = (value: number) =>
  Math.min(MAX_SCALE, Math.max(MIN_SCALE, Math.round(value * 100) / 100));

interface PropertyLightboxProps {
  media: PropertyMedia[];
  index: number;
  onClose: () => void;
  onIndexChange: (index: number) => void;
  /** Wołane, gdy zdjęcie nie chce się załadować — patrz komentarz przy `onError`. */
  onExpired?: () => void;
}

/**
 * Podgląd zdjęcia na całym ekranie: przewijanie w lewo/prawo, zoom i pasek
 * miniatur. Pokazuje `url` (pełny rozmiar) — miniatury zostają tylko na dole.
 *
 * Renderowany przez portal do `document.body`, bo galeria stoi w kontenerze
 * `sticky`: `position: fixed` wewnątrz takiego rodzica potrafi się przykleić
 * do niego zamiast do okna i podgląd wylądowałby w prawej kolumnie.
 */
export function PropertyLightbox({
  media,
  index,
  onClose,
  onIndexChange,
  onExpired,
}: PropertyLightboxProps) {
  const [scale, setScale] = useState(1);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const surfaceRef = useRef<HTMLDivElement>(null);
  const dialogRef = useRef<HTMLDivElement>(null);
  const activeThumbRef = useRef<HTMLButtonElement>(null);
  const dragRef = useRef<{ x: number; y: number; ox: number; oy: number } | null>(
    null,
  );
  // Zdjęcia, dla których prosiliśmy już o świeże linki. Bez tego plik trwale
  // uszkodzony zapętliłby odświeżanie galerii.
  const retriedRef = useRef<Set<string>>(new Set());

  const item = media[index];
  const hasPrev = index > 0;
  const hasNext = index < media.length - 1;

  const resetZoom = useCallback(() => {
    setScale(1);
    setOffset({ x: 0, y: 0 });
  }, []);

  const go = useCallback(
    (next: number) => {
      if (next < 0 || next >= media.length) return;
      onIndexChange(next);
    },
    [media.length, onIndexChange],
  );

  // Zmiana zdjęcia zaczyna od nowa — inaczej kolejne otwierałoby się przesunięte
  // w miejsce, które miało sens tylko dla poprzedniego kadru.
  useEffect(resetZoom, [index, resetZoom]);

  // Klawiatura: strzałki przewijają, Esc zamyka, +/- to zoom.
  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      switch (event.key) {
        case "Escape":
          onClose();
          break;
        case "ArrowLeft":
          go(index - 1);
          break;
        case "ArrowRight":
          go(index + 1);
          break;
        case "+":
        case "=":
          setScale((s) => clamp(s + STEP));
          break;
        case "-":
          setScale((s) => {
            const next = clamp(s - STEP);
            if (next === MIN_SCALE) setOffset({ x: 0, y: 0 });
            return next;
          });
          break;
        case "0":
          resetZoom();
          break;
        default:
          return;
      }
      event.preventDefault();
    }

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [go, index, onClose, resetZoom]);

  // Zoom kółkiem. Listener natywny z `passive: false`, bo React wiesza `wheel`
  // jako pasywny i `preventDefault()` w `onWheel` nie zatrzymałby przewijania.
  useEffect(() => {
    const node = surfaceRef.current;
    if (!node) return;

    function onWheel(event: WheelEvent) {
      event.preventDefault();
      setScale((s) => {
        const next = clamp(s - Math.sign(event.deltaY) * STEP);
        if (next === MIN_SCALE) setOffset({ x: 0, y: 0 });
        return next;
      });
    }

    node.addEventListener("wheel", onWheel, { passive: false });
    return () => node.removeEventListener("wheel", onWheel);
  }, []);

  // Na czas podglądu blokujemy przewijanie strony pod spodem i zabieramy focus,
  // żeby strzałki nie ruszały jednocześnie listą pod overlayem.
  useEffect(() => {
    const previous = document.body.style.overflow;
    const restoreFocus = document.activeElement as HTMLElement | null;
    document.body.style.overflow = "hidden";
    dialogRef.current?.focus();
    return () => {
      document.body.style.overflow = previous;
      restoreFocus?.focus?.();
    };
  }, []);

  // Aktywna miniatura ma zostać widoczna także przy pięćdziesięciu zdjęciach.
  useEffect(() => {
    activeThumbRef.current?.scrollIntoView({ block: "nearest", inline: "center" });
  }, [index]);

  if (!item) return null;

  function startDrag(event: React.PointerEvent<HTMLImageElement>) {
    if (scale === MIN_SCALE) return;
    dragRef.current = { x: event.clientX, y: event.clientY, ox: offset.x, oy: offset.y };
    event.currentTarget.setPointerCapture(event.pointerId);
  }

  function onDrag(event: React.PointerEvent<HTMLImageElement>) {
    const start = dragRef.current;
    if (!start) return;
    setOffset({
      x: start.ox + (event.clientX - start.x),
      y: start.oy + (event.clientY - start.y),
    });
  }

  function endDrag(event: React.PointerEvent<HTMLImageElement>) {
    if (!dragRef.current) return;
    dragRef.current = null;
    event.currentTarget.releasePointerCapture(event.pointerId);
  }

  return createPortal(
    <div
      ref={dialogRef}
      role="dialog"
      aria-modal="true"
      aria-label={`Zdjęcie ${index + 1} z ${media.length}`}
      tabIndex={-1}
      className="fixed inset-0 z-50 flex flex-col bg-ink/95 outline-none"
    >
      <header className="flex items-center justify-between gap-4 px-4 py-3 text-white">
        <div className="min-w-0">
          <span className="text-[13px] font-medium tabular-nums">
            {index + 1} / {media.length}
          </span>
          {item.caption && (
            <span className="ml-3 truncate text-[13px] text-white/70">
              {item.caption}
            </span>
          )}
        </div>

        <div className="flex shrink-0 items-center gap-1">
          <IconButton
            label="Pomniejsz"
            disabled={scale === MIN_SCALE}
            onClick={() =>
              setScale((s) => {
                const next = clamp(s - STEP);
                if (next === MIN_SCALE) setOffset({ x: 0, y: 0 });
                return next;
              })
            }
          >
            <Minus className="size-4" strokeWidth={2} />
          </IconButton>
          <span className="w-12 text-center text-[12px] tabular-nums text-white/70">
            {Math.round(scale * 100)}%
          </span>
          <IconButton
            label="Powiększ"
            disabled={scale === MAX_SCALE}
            onClick={() => setScale((s) => clamp(s + STEP))}
          >
            <Plus className="size-4" strokeWidth={2} />
          </IconButton>
          <IconButton
            label="Rozmiar pierwotny"
            disabled={scale === MIN_SCALE}
            onClick={resetZoom}
          >
            <RotateCcw className="size-4" strokeWidth={2} />
          </IconButton>
          <IconButton label="Zamknij podgląd" onClick={onClose}>
            <X className="size-5" strokeWidth={2} />
          </IconButton>
        </div>
      </header>

      <div
        ref={surfaceRef}
        className="relative flex-1 overflow-hidden"
        onClick={(event) => {
          if (event.target === event.currentTarget) onClose();
        }}
      >
        <img
          key={item.id}
          src={item.url}
          alt={item.caption ?? item.fileName}
          draggable={false}
          onPointerDown={startDrag}
          onPointerMove={onDrag}
          onPointerUp={endDrag}
          onPointerCancel={endDrag}
          // Linki do storage'u są podpisane na ~15 minut, a kartę oferty da się
          // trzymać otwartą dłużej. Pierwsze niepowodzenie traktujemy więc jako
          // wygaśnięcie i prosimy stronę o świeży komplet URL-i.
          onError={() => {
            if (retriedRef.current.has(item.id)) return;
            retriedRef.current.add(item.id);
            onExpired?.();
          }}
          className="absolute inset-0 m-auto max-h-full max-w-full object-contain select-none"
          style={{
            transform: `translate(${offset.x}px, ${offset.y}px) scale(${scale})`,
            cursor: scale > MIN_SCALE ? "grab" : "default",
            touchAction: "none",
          }}
        />

        {hasPrev && (
          <NavButton side="left" label="Poprzednie zdjęcie" onClick={() => go(index - 1)}>
            <ChevronLeft className="size-6" strokeWidth={2} />
          </NavButton>
        )}
        {hasNext && (
          <NavButton side="right" label="Następne zdjęcie" onClick={() => go(index + 1)}>
            <ChevronRight className="size-6" strokeWidth={2} />
          </NavButton>
        )}
      </div>

      {media.length > 1 && (
        <footer className="flex gap-2 overflow-x-auto px-4 py-3">
          {media.map((thumb, i) => (
            <button
              key={thumb.id}
              ref={i === index ? activeThumbRef : undefined}
              type="button"
              onClick={() => go(i)}
              aria-label={`Zdjęcie ${i + 1}`}
              aria-current={i === index}
              className={
                "h-14 w-20 shrink-0 overflow-hidden rounded-sm border-2 transition-opacity " +
                (i === index
                  ? "border-white"
                  : "border-transparent opacity-60 hover:opacity-100")
              }
            >
              <img
                src={thumb.thumbnailUrl}
                alt=""
                loading="lazy"
                className="h-full w-full object-cover"
              />
            </button>
          ))}
        </footer>
      )}
    </div>,
    document.body,
  );
}

function IconButton({
  label,
  onClick,
  disabled,
  children,
}: {
  label: string;
  onClick: () => void;
  disabled?: boolean;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      title={label}
      className="flex size-8 items-center justify-center rounded-md text-white/80 transition-colors hover:bg-white/10 hover:text-white disabled:opacity-30 disabled:hover:bg-transparent"
    >
      {children}
    </button>
  );
}

function NavButton({
  side,
  label,
  onClick,
  children,
}: {
  side: "left" | "right";
  label: string;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      title={label}
      className={
        "absolute top-1/2 flex size-11 -translate-y-1/2 items-center justify-center rounded-full bg-ink/60 text-white/90 transition-colors hover:bg-ink/80 hover:text-white " +
        (side === "left" ? "left-4" : "right-4")
      }
    >
      {children}
    </button>
  );
}
