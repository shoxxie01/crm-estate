import { useEffect, useId, useRef } from "react";
import { createPortal } from "react-dom";
import { Button } from "./Button";

interface ConfirmDialogProps {
  /** Pytanie w nagłówku — pełne zdanie, nie sama nazwa operacji. */
  title: string;
  /** Czego dotyczy i co się stanie. Opcjonalne. */
  description?: React.ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  /** Trwa operacja — oba przyciski blokujemy, żeby nie poszła dwa razy. */
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/**
 * Potwierdzenie operacji nieodwracalnej.
 *
 * <p>Renderowane portalem do `body`: okno ma przykryć całą stronę, a wywołanie
 * potrafi stać w komórce tabeli albo w przyklejonej kolumnie, gdzie `fixed`
 * przykleiłby się do rodzica zamiast do okna.
 *
 * <p>Focus startuje na „Anuluj" — okno otwiera się po kliknięciu kosza, więc
 * Enter odruchowo wciśnięty zaraz po nim ma anulować, a nie kasować.
 */
export function ConfirmDialog({
  title,
  description,
  confirmLabel = "Usuń",
  cancelLabel = "Anuluj",
  busy = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const titleId = useId();

  // Wołający zwykle podaje `onCancel` jako świeżą funkcję przy każdym renderze.
  // Trzymamy ją w ref, żeby efekt biegł raz na otwarcie — inaczej co render
  // zdejmowałby blokadę przewijania i przerzucał focus z powrotem na kosz.
  const cancelRef = useRef(onCancel);
  cancelRef.current = onCancel;

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    const restoreFocus = document.activeElement as HTMLElement | null;
    document.body.style.overflow = "hidden";

    function onKeyDown(event: KeyboardEvent) {
      if (event.key !== "Escape") return;
      event.preventDefault();
      cancelRef.current();
    }

    window.addEventListener("keydown", onKeyDown);
    return () => {
      window.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = previousOverflow;
      restoreFocus?.focus?.();
    };
  }, []);

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-ink/20 p-4"
      onClick={(event) => {
        if (event.target === event.currentTarget && !busy) onCancel();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="card w-full max-w-sm p-5"
      >
        <h2
          id={titleId}
          className="text-[14px] font-semibold tracking-tight text-ink"
        >
          {title}
        </h2>

        {description && (
          <div className="mt-2 text-[13px] leading-relaxed text-ink-secondary">
            {description}
          </div>
        )}

        <div className="mt-5 flex justify-end gap-2">
          <Button
            autoFocus
            variant="ghost"
            size="sm"
            onClick={onCancel}
            disabled={busy}
          >
            {cancelLabel}
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={onConfirm}
            disabled={busy}
            className="border-critical/40 text-critical hover:bg-critical/8"
          >
            {busy ? "Usuwanie…" : confirmLabel}
          </Button>
        </div>
      </div>
    </div>,
    document.body,
  );
}
