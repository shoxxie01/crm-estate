package pl.delta.crm.property.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import pl.delta.crm.property.dictionary.Currency;

import java.math.BigDecimal;

public record PricingRequest(

        @NotNull(message = "Podaj cenę.")
        @DecimalMin(value = "0.01", message = "Cena musi być większa od zera.")
        @Digits(integer = 12, fraction = 2)
        BigDecimal price,

        Currency priceCurrency,

        Boolean priceNegotiable,

        /* Czynsz administracyjny — portale pokazują go osobno od ceny. */
        @DecimalMin(value = "0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal rent,

        Currency rentCurrency,

        Boolean priceIncludesRent,

        @DecimalMin(value = "0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal deposit,

        Currency depositCurrency,

        @DecimalMin(value = "0.00")
        @Digits(integer = 3, fraction = 2)
        BigDecimal commissionPercent
) {
}
