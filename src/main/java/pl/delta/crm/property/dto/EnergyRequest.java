package pl.delta.crm.property.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import pl.delta.crm.property.dictionary.EnergyClass;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Świadectwo charakterystyki energetycznej — obowiązkowe przy sprzedaży
 * i najmie od 28.04.2023, poza budynkami zwolnionymi z obowiązku.
 */
public record EnergyRequest(

        @DecimalMin(value = "0.00")
        @Digits(integer = 5, fraction = 2)
        BigDecimal energyPrimary,

        @DecimalMin(value = "0.00")
        @Digits(integer = 5, fraction = 2)
        BigDecimal energyFinal,

        EnergyClass energyClass,

        @Size(max = 60)
        String certificateNumber,

        LocalDate issuedAt,
        LocalDate validUntil,

        Boolean exempt,

        @Size(max = 200)
        String exemptNote
) {
}
