import { Badge } from "../../components/ui/Badge";

/**
 * Rola klienta wyprowadzona z powierzonych ofert — nie z pola na kliencie.
 * Sprzedający, wynajmujący, oba naraz albo (gdy nie ma jeszcze żadnej oferty)
 * właściciel bez zlecenia.
 */
export function ClientIntent({
  sellCount,
  rentCount,
}: {
  sellCount: number;
  rentCount: number;
}) {
  if (sellCount === 0 && rentCount === 0) {
    return (
      <span className="text-[12px] text-ink-muted">Brak zlecenia</span>
    );
  }

  return (
    <span className="inline-flex flex-wrap gap-1">
      {sellCount > 0 && (
        <Badge tone="accent">
          Sprzedający{sellCount > 1 ? ` · ${sellCount}` : ""}
        </Badge>
      )}
      {rentCount > 0 && (
        <Badge tone="warning">
          Wynajmujący{rentCount > 1 ? ` · ${rentCount}` : ""}
        </Badge>
      )}
    </span>
  );
}
