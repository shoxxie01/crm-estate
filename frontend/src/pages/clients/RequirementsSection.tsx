import { useState } from "react";
import { Pencil, Plus, Search, Trash2 } from "lucide-react";
import {
  changeRequirementStatus,
  deleteRequirement,
  type ClientRequirement,
} from "../../api/clients";
import { ApiError } from "../../api/client";
import { Button } from "../../components/ui/Button";
import { ConfirmDialog } from "../../components/ui/ConfirmDialog";
import { cn } from "../../lib/cn";
import { RequirementForm } from "./RequirementForm";
import { RequirementMatches } from "./RequirementMatches";
import {
  RequirementFacts,
  RequirementHeadline,
  useRequirementDictionaries,
  type RequirementLabels,
} from "./RequirementDetails";

interface RequirementsSectionProps {
  clientId: string;
  requirements: ClientRequirement[];
  /** Po każdej zmianie karta klienta wczytuje się na nowo. Zmienia się też rola. */
  onChanged: () => Promise<void>;
}

/**
 * Poszukiwania na karcie klienta: czego klient szuka jako kupujący albo
 * najemca. Aktywne liczą się do roli, reszta zostaje jako historia.
 */
export function RequirementsSection({
  clientId,
  requirements,
  onChanged,
}: RequirementsSectionProps) {
  const { propertyDict, clientDict, labels } = useRequirementDictionaries();
  const [editing, setEditing] = useState<ClientRequirement | "new" | null>(null);
  const [removing, setRemoving] = useState<ClientRequirement | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const ready = propertyDict !== null && clientDict !== null;

  async function run(action: () => Promise<unknown>, failure: string) {
    setBusy(true);
    setError(null);
    try {
      await action();
      await onChanged();
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card">
      <header className="flex flex-wrap items-end justify-between gap-3 border-b border-line px-4 py-3">
        <div>
          <h2 className="text-[13px] font-semibold tracking-tight text-ink">
            Poszukiwania
          </h2>
          <p className="mt-0.5 text-[12px] text-ink-muted">
            Aktywne decydują, czy klient jest kupującym, czy najemcą.
          </p>
        </div>
        <Button
          type="button"
          variant="secondary"
          size="sm"
          onClick={() => setEditing("new")}
          disabled={!ready || busy}
        >
          <Plus className="size-4" strokeWidth={2} />
          Dodaj poszukiwanie
        </Button>
      </header>

      {error && (
        <p className="mx-4 mt-3 rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {error}
        </p>
      )}

      {requirements.length === 0 ? (
        <div className="px-4 py-8 text-center">
          <Search className="mx-auto mb-2 size-5 text-ink-muted" strokeWidth={1.5} />
          <p className="text-[13px] text-ink-secondary">
            Klient niczego nie szuka.
          </p>
          <p className="mt-0.5 text-[12px] text-ink-muted">
            Gdy zgłosi się po nieruchomość, zapisz tu jego kryteria.
          </p>
        </div>
      ) : (
        <ul className="flex flex-col gap-3 p-4">
          {requirements.map((requirement) => (
            <li key={requirement.id}>
              <RequirementCard
                requirement={requirement}
                labels={labels}
                statusOptions={clientDict?.requirementStatus ?? []}
                disabled={busy}
                onEdit={() => setEditing(requirement)}
                onRemove={() => setRemoving(requirement)}
                onStatus={(status) =>
                  run(
                    () => changeRequirementStatus(clientId, requirement.id, status),
                    "Nie udało się zmienić stanu poszukiwania.",
                  )
                }
              />
            </li>
          ))}
        </ul>
      )}

      {editing && propertyDict && clientDict && (
        <RequirementForm
          clientId={clientId}
          initial={editing === "new" ? null : editing}
          propertyDictionaries={propertyDict}
          clientDictionaries={clientDict}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            void onChanged();
          }}
        />
      )}

      {removing && (
        <ConfirmDialog
          title="Usunąć to poszukiwanie?"
          description="Jeśli klient kupił albo zrezygnował, lepiej zmienić stan na „Zrealizowane” lub „Nieaktualne”. Historia zostanie na karcie."
          busy={busy}
          onCancel={() => setRemoving(null)}
          onConfirm={() =>
            run(
              () => deleteRequirement(clientId, removing.id),
              "Nie udało się usunąć poszukiwania.",
            ).then(() => setRemoving(null))
          }
        />
      )}
    </section>
  );
}

function RequirementCard({
  requirement: r,
  labels,
  statusOptions,
  disabled,
  onEdit,
  onRemove,
  onStatus,
}: {
  requirement: ClientRequirement;
  labels: RequirementLabels;
  statusOptions: { value: string; label: string }[];
  disabled: boolean;
  onEdit: () => void;
  onRemove: () => void;
  onStatus: (status: string) => void;
}) {
  const active = r.status === "ACTIVE";

  return (
    <article
      className={cn(
        "rounded-md border border-line p-3.5",
        active ? "bg-surface" : "bg-subtle/60",
      )}
    >
      <header className="flex flex-wrap items-start justify-between gap-2">
        <RequirementHeadline criteria={r} labels={labels} muted={!active} />

        <div className="flex items-center gap-1">
          <select
            aria-label="Stan poszukiwania"
            value={r.status}
            disabled={disabled}
            onChange={(e) => onStatus(e.target.value)}
            className="h-8 rounded-md border border-line bg-surface px-2 text-[12px] text-ink hover:border-line-strong focus:border-accent focus:ring-2 focus:ring-accent-ring/50 focus:outline-none disabled:opacity-55"
          >
            {statusOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={onEdit}
            disabled={disabled}
            aria-label="Edytuj poszukiwanie"
          >
            <Pencil className="size-4" strokeWidth={2} />
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={onRemove}
            disabled={disabled}
            aria-label="Usuń poszukiwanie"
            className="hover:bg-critical/8 hover:text-critical"
          >
            <Trash2 className="size-4" strokeWidth={2} />
          </Button>
        </div>
      </header>

      <RequirementFacts criteria={r} labels={labels} />

      {r.notes && (
        <p className="mt-3 text-[13px] whitespace-pre-line text-ink-secondary">
          {r.notes}
        </p>
      )}

      <p className="mt-3 text-[11px] text-ink-muted">
        Dodane przez {r.createdByName},{" "}
        {new Date(r.createdAt).toLocaleDateString("pl-PL")}
      </p>

      {/* Tylko dla aktywnych. Dla wstrzymanych i zamkniętych lista ofert
          byłaby szumem, a każda to osobne zapytanie. */}
      {active && (
        <RequirementMatches
          clientId={r.clientId}
          requirementId={r.id}
          version={r.updatedAt}
        />
      )}
    </article>
  );
}
