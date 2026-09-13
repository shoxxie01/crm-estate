import type { ChangeEvent, FocusEventHandler } from "react";
import { Input } from "./Input";
import { COUNTRY_PREFIX, isInternational, maskPhone } from "../../lib/phone";

interface PhoneInputProps {
  label: string;
  /** Wartość pola: `605 405 932` albo numer zagraniczny z plusem. Patrz lib/phone.ts. */
  value: string;
  onChange: (value: string) => void;
  onBlur?: FocusEventHandler<HTMLInputElement>;
  error?: string;
  hint?: string;
  required?: boolean;
}

/**
 * Telefon z kierunkowym +48 na stałe przy polu. Wpisanie „+" na początku
 * przełącza pole na numer zagraniczny, wyczyszczenie — z powrotem na krajowy.
 */
export function PhoneInput({ value, onChange, ...props }: PhoneInputProps) {
  const international = isInternational(value);

  // Jak przy kodzie pocztowym: wartość i karetkę ustawiamy na elemencie przed
  // setState, żeby React nie przestawiał pola i karetka nie uciekała na koniec.
  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    const input = event.target;
    const inputType = (event.nativeEvent as InputEvent).inputType;
    const next = maskPhone(
      input.value,
      input.selectionStart ?? input.value.length,
      value,
      inputType === "deleteContentForward",
    );
    input.value = next.value;
    input.setSelectionRange(next.caret, next.caret);
    onChange(next.value);
  };

  return (
    <Input
      {...props}
      type="tel"
      inputMode="tel"
      autoComplete="tel"
      value={value}
      onChange={handleChange}
      leading={international ? undefined : COUNTRY_PREFIX}
      placeholder={international ? "+49 151 2345 6789" : "605 405 932"}
      // W trybie krajowym długość pilnuje maska. maxLength uciąłby wklejany
      // numer z kierunkowym, zanim maska zdąży go rozpoznać.
      maxLength={international ? 30 : undefined}
    />
  );
}
