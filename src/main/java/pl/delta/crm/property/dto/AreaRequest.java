package pl.delta.crm.property.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AreaRequest(

        @NotNull(message = "Podaj powierzchnię.")
        @DecimalMin(value = "0.01", message = "Powierzchnia musi być większa od zera.")
        @Digits(integer = 8, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal totalArea,

        @DecimalMin(value = "0.01")
        @Digits(integer = 8, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal usableArea,

        @DecimalMin(value = "0.01")
        @Digits(integer = 10, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal plotArea,

        @Min(value = 1, message = "Liczba pokoi musi być dodatnia.")
        @Max(value = 100)
        Short roomsCount,

        @Min(0) @Max(50)
        Short bathroomsCount,

        /* -1 = suterena, 0 = parter, dalej numer piętra. Górny limit 160 — powyżej
           najwyższych zamieszkałych budynków świata (Burj Khalifa). */
        @Min(value = -1, message = "Najniższa dopuszczalna wartość to -1 (suterena).")
        @Max(value = 160, message = "Piętro nie może być wyższe niż 160.")
        Short floorNo,

        @Min(1) @Max(200)
        Short buildingFloorsCount,

        @DecimalMin(value = "0.5")
        @Digits(integer = 3, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal ceilingHeight
) {
}
