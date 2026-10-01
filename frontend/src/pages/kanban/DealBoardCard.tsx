import type { DragEvent } from "react";
import { AlertTriangle, Building2, Clock, FileText, User, Users } from "lucide-react";
import type { DealCard } from "../../api/deals";
import { formatCompactPLN } from "../../lib/format";
import { cn } from "../../lib/cn";
import { eventIcon, typeInk } from "../calendar/eventMeta";
import {
  DEADLINE_WARN_DAYS,
  daysInStage,
  daysLabel,
  daysUntil,
  deadlineTone,
  deadlineWhen,
  initials,
  staleness,
  whenLabel,
} from "./dealMeta";

interface DealBoardCardProps {
  card: DealCard;
  selected: boolean;
  label: (value: string) => string;
  onOpen: () => void;
  onDragStart: (event: DragEvent<HTMLElement>) => void;
  onDragEnd: () => void;
}

/**
 * Karta transakcji na tablicy.
 *
 * Najważniejsza jest dolna linia: najbliższy termin z kalendarza albo
 * ostrzeżenie, że nikt nie zaplanował kolejnego kroku. Transakcje nie giną
 * na negocjacjach, tylko w ciszy między jednym telefonem a drugim. Karta ma
 * tę ciszę pokazywać.
 */
export function DealBoardCard({
  card,
  selected,
  label,
  onOpen,
  onDragStart,
  onDragEnd,
}: DealBoardCardProps) {
  // `!= null`, a nie `!== null`: puste pola nie przychodzą w JSON-ie wcale.
  const closed = card.closedAt != null;
  const days = daysInStage(card);
  const stale = staleness(card);
  const next = card.nextEvent;
  const NextIcon = next ? eventIcon(next.type) : null;
  // Plakietka terminu umownego pojawia się na cztery tygodnie przed nim.
  // Wcześniej to szum, a od dwóch tygodni robi się żółta, od trzech dni czerwona.
  const deadline = closed ? null : card.nextDeadline;
  const deadlineDays = deadline ? daysUntil(deadline.dueDate) : null;

  return (
    <article
      draggable
      onDragStart={onDragStart}
      onDragEnd={onDragEnd}
      className={cn(
        "group cursor-grab rounded-md border bg-surface text-[12px] transition-colors active:cursor-grabbing",
        selected
          ? "border-accent ring-1 ring-accent-ring"
          : "border-line hover:border-line-strong",
      )}
    >
      <button
        type="button"
        onClick={onOpen}
        className="flex w-full flex-col gap-1 px-2.5 pt-2 pb-1.5 text-left"
      >
        <div className="flex items-start justify-between gap-1.5">
          <h3 className="line-clamp-2 font-medium leading-snug text-ink">{card.title}</h3>
          <span
            className="flex size-5 shrink-0 items-center justify-center rounded-full bg-subtle text-[9px] font-semibold text-ink-secondary"
            title={card.agentName}
            aria-label={`Agent: ${card.agentName}`}
          >
            {initials(card.agentName)}
          </span>
        </div>

        {(card.propertyReference || card.propertyAddress) && (
          <p className="flex items-center gap-1.5 text-[11px] text-ink-secondary">
            <Building2 className="size-3 shrink-0 text-ink-muted" strokeWidth={2} />
            <span className="truncate">
              {card.propertyReference && (
                <span className="tabular-nums">{card.propertyReference}</span>
              )}
              {card.propertyReference && card.propertyAddress && " · "}
              {card.propertyAddress}
            </span>
          </p>
        )}

        {card.clientName && (
          <p className="flex items-center gap-1.5 text-[11px] text-ink-secondary">
            <User className="size-3 shrink-0 text-ink-muted" strokeWidth={2} />
            <span className="truncate">
              {card.clientName}
              {card.buyerName && <span className="text-ink-muted"> → {card.buyerName}</span>}
            </span>
          </p>
        )}

        {card.interestCount > 0 && (
          <p className="flex items-center gap-1.5 text-[11px] text-ink-secondary">
            <Users className="size-3 shrink-0 text-ink-muted" strokeWidth={2} />
            <span className="truncate">
              {interestedLabel(card.interestCount)}
              {card.offerCount > 0 && (
                <span className="font-medium text-ink">
                  {" "}· {card.offerCount} {card.offerCount === 1 ? "oferta" : "oferty"}
                  {card.bestOffer != null && ` (do ${formatCompactPLN(card.bestOffer)})`}
                </span>
              )}
            </span>
          </p>
        )}

        <div className="flex items-center justify-between gap-2">
          <span className="font-semibold text-ink tabular-nums">
            {card.value != null ? formatCompactPLN(card.value) : "-"}
          </span>
          {card.transactionType && (
            <span className="text-[11px] text-ink-muted">
              {card.transactionType === "RENT" ? "Najem" : "Sprzedaż"}
            </span>
          )}
        </div>

        {deadline && deadlineDays !== null && deadlineDays <= DEADLINE_WARN_DAYS * 2 && (
          <p
            className={cn(
              "flex items-center gap-1.5 text-[11px]",
              deadlineTone(deadlineDays) === "critical"
                ? "font-medium text-critical"
                : deadlineTone(deadlineDays) === "warning"
                  ? "font-medium text-warning-ink"
                  : "text-ink-secondary",
            )}
          >
            <FileText className="size-3 shrink-0" strokeWidth={2} />
            <span className="truncate">
              {label(deadline.type)} · {deadlineWhen(deadlineDays)}
            </span>
          </p>
        )}

        {card.lostReason && (
          <p className="text-[11px] text-critical">{label(card.lostReason)}</p>
        )}
      </button>

      {!closed && (
        <footer className="flex flex-wrap items-center justify-between gap-x-2 gap-y-0.5 border-t border-line px-2.5 py-1 text-[11px]">
          {next && NextIcon ? (
            <span className="flex min-w-0 items-center gap-1.5 text-ink-secondary">
              <NextIcon className={cn("size-3.5 shrink-0", typeInk(next.type))} strokeWidth={2} />
              <span className="truncate">
                {label(next.type)} · {whenLabel(next.startsAt, next.allDay)}
              </span>
            </span>
          ) : (
            <span className="flex items-center gap-1.5 font-medium text-warning-ink">
              <AlertTriangle className="size-3 shrink-0" strokeWidth={2} />
              Brak następnego kroku
            </span>
          )}

          <span
            className={cn(
              "flex shrink-0 items-center gap-1 tabular-nums",
              stale === "dead"
                ? "font-medium text-critical"
                : stale === "stale"
                  ? "font-medium text-warning-ink"
                  : "text-ink-muted",
            )}
            title="Czas w obecnym etapie"
          >
            <Clock className="size-3" strokeWidth={2} />
            {daysLabel(days)}
          </span>
        </footer>
      )}
    </article>
  );
}

/** „1 zainteresowany", „3 zainteresowanych". */
function interestedLabel(count: number): string {
  return count === 1 ? "1 zainteresowany" : `${count} zainteresowanych`;
}
