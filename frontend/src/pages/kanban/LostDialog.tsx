import { useState } from "react";
import { createPortal } from "react-dom";
import type { DictionaryEntry } from "../../api/properties";
import { Button } from "../../components/ui/Button";
import { Select } from "../../components/ui/Select";
import { Textarea } from "../../components/ui/Textarea";

interface LostDialogProps {
  title: string;
  reasons: DictionaryEntry[];
  busy: boolean;
  error: string | null;
  onConfirm: (reason: string, note: string) => void;
  onCancel: () => void;
}

/**
 * Pytanie o powód przegranej. Wyskakuje po upuszczeniu karty na „Przegrane".
 * Bez powodu kolumna przegranych to cmentarz, z którego nic nie wynika.
 */
export function LostDialog({ title, reasons, busy, error, onConfirm, onCancel }: LostDialogProps) {
  const [reason, setReason] = useState("");
  const [note, setNote] = useState("");
  const [touched, setTouched] = useState(false);

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-start justify-center bg-scrim/20 p-4 sm:p-8"
      role="dialog"
      aria-modal="true"
      aria-label="Powód przegranej"
      onKeyDown={(event) => {
        if (event.key !== "Escape") return;
        // Escape zamyka tylko to okno, nie panel karty pod spodem.
        event.stopPropagation();
        onCancel();
      }}
    >
      <form
        className="card w-full max-w-md"
        onSubmit={(event) => {
          event.preventDefault();
          setTouched(true);
          if (reason) onConfirm(reason, note);
        }}
      >
        <header className="border-b border-line px-4 py-3">
          <h2 className="text-[14px] font-semibold tracking-tight text-ink">
            Dlaczego transakcja przepadła?
          </h2>
          <p className="mt-0.5 truncate text-[12px] text-ink-muted">{title}</p>
        </header>

        <div className="flex flex-col gap-3 p-4">
          <Select
            label="Powód"
            required
            autoFocus
            value={reason}
            onChange={(changed) => setReason(changed.target.value)}
            placeholder="Wybierz"
            options={reasons}
            error={touched && !reason ? "Wybierz powód przegranej." : undefined}
          />
          <Textarea
            label="Notatka"
            rows={2}
            value={note}
            onChange={(changed) => setNote(changed.target.value)}
            placeholder="Np. które biuro, jaka była rozbieżność w cenie"
          />
          {error && (
            <p className="rounded-md border border-critical/30 bg-critical/8 px-2.5 py-1.5 text-[12px] text-critical">
              {error}
            </p>
          )}
        </div>

        <footer className="flex justify-end gap-2 border-t border-line px-4 py-3">
          <Button type="button" variant="secondary" onClick={onCancel} disabled={busy}>
            Anuluj
          </Button>
          <Button type="submit" disabled={busy}>
            {busy ? "Zapisywanie…" : "Oznacz jako przegraną"}
          </Button>
        </footer>
      </form>
    </div>,
    document.body,
  );
}
