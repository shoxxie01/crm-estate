package pl.delta.crm.property.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import pl.delta.crm.property.dictionary.Currency;

import java.math.BigDecimal;

public record PricingRequest(

        @NotNull(message = "Podaj cenę.")
        @DecimalMin(value = "0.01", message = "Cena musi być większa od zera.")
        @Digits(integer = 12, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal price,

        Currency priceCurrency,

        Boolean priceNegotiable,

        /* Czynsz administracyjny. Portale pokazują go osobno od ceny. */
        @DecimalMin(value = "0.00")
        @Digits(integer = 10, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal rent,

        Currency rentCurrency,

        Boolean priceIncludesRent,

        @DecimalMin(value = "0.00")
        @Digits(integer = 10, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal deposit,

        Currency depositCurrency,

        @DecimalMin(value = "0.00")
        @DecimalMax(value = "100.00", message = "Prowizja nie może przekraczać 100%.")
        @Digits(integer = 3, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal commissionPercent,

        /* Cena za m². W formularzu pole wymagane (wpisywane ręcznie albo liczone
           z ceny i powierzchni). W API opcjonalne: gdy go brak, serwis wylicza je
           sam z ceny i powierzchni całkowitej, więc wartość zawsze trafia do bazy. */
        @DecimalMin(value = "0.01", message = "Cena za m² musi być większa od zera.")
        @Digits(integer = 12, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal pricePerM2
) {
}
