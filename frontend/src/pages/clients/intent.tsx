import { Badge } from "../../components/ui/Badge";

/**
 * Role klienta wyprowadzone z jego ofert i poszukiwań. Nie z pola na kliencie.
 * Sprzedający / wynajmujący z powierzonych ofert, kupujący / najemca
 * z aktywnych poszukiwań. Dowolna kombinacja naraz albo (gdy nie ma niczego)
 * kontakt bez zlecenia.
 */
export function ClientIntent({
  sellCount,
  rentCount,
  buyerCount,
  tenantCount,
}: {
  sellCount: number;
  rentCount: number;
  buyerCount: number;
  tenantCount: number;
}) {
  if (sellCount + rentCount + buyerCount + tenantCount === 0) {
    return (
      <span className="text-[12px] text-ink-muted">Brak zlecenia</span>
    );
  }

  return (
    <span className="inline-flex flex-wrap gap-1">
      {sellCount > 0 && <Role tone="accent" label="Sprzedający" count={sellCount} />}
      {rentCount > 0 && <Role tone="warning" label="Wynajmujący" count={rentCount} />}
      {buyerCount > 0 && <Role tone="good" label="Kupujący" count={buyerCount} />}
      {tenantCount > 0 && <Role tone="neutral" label="Najemca" count={tenantCount} />}
    </span>
  );
}

function Role({
  tone,
  label,
  count,
}: {
  tone: "accent" | "warning" | "good" | "neutral";
  label: string;
  count: number;
}) {
  return (
    <Badge tone={tone}>
      {label}
      {count > 1 ? ` · ${count}` : ""}
    </Badge>
  );
}
