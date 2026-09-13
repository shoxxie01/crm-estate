package pl.delta.crm.inquiry.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.delta.crm.property.dictionary.PropertyType;

import java.math.BigDecimal;

/**
 * Nieruchomość, którą właściciel chce sprzedać przez biuro — tyle, ile umie
 * podać w formularzu. Resztę (adres, stan, zdjęcia) agent ustala przy rozmowie
 * albo na oględzinach, zanim założy ofertę.
 */
public record SaleOfferRequest(

        @NotNull(message = "Wybierz, co chcesz sprzedać.")
        PropertyType propertyType,

        @NotBlank(message = "Podaj miejscowość.")
        @Size(max = 80, message = "Nazwa miejscowości jest zbyt długa.")
        String city,

        @Size(max = 64, message = "Nazwa dzielnicy jest zbyt długa.")
        String district,

        @DecimalMin(value = "0.01", message = "Powierzchnia musi być większa od zera.")
        @Digits(integer = 8, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal area,

        @Min(value = 1, message = "Liczba pokoi musi być dodatnia.")
        @Max(value = 100, message = "Liczba pokoi jest zbyt duża.")
        Short roomsCount,

        @DecimalMin(value = "0", message = "Kwota nie może być ujemna.")
        @Digits(integer = 12, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal expectedPrice
) {
}
