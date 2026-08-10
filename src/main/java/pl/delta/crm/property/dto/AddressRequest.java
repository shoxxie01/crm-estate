package pl.delta.crm.property.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pl.delta.crm.property.dictionary.Voivodeship;

import java.math.BigDecimal;

/**
 * Przy zapisie wymagane są tylko województwo i miejscowość.
 *
 * <p>Otodom potrzebuje pełnego kompletu (kraj + województwo + powiat + miasto),
 * ale brak powiatu blokuje publikację, a nie samo istnienie oferty — pilnuje
 * tego {@code Property.readyForExport()}.
 */
public record AddressRequest(

        @NotNull(message = "Wybierz województwo.")
        Voivodeship voivodeship,

        /* Opcjonalny — wymagany dopiero przy eksporcie na portal. */
        @Size(max = 80)
        String county,

        @Size(max = 80)
        String commune,

        @NotBlank(message = "Podaj miejscowość.")
        @Size(max = 80)
        String city,

        @Size(max = 64)
        String district,

        @Size(max = 64)
        String street,

        @Size(max = 20)
        String buildingNumber,

        @Size(max = 20)
        String apartmentNumber,

        @Pattern(regexp = "^\\d{2}-\\d{3}$", message = "Kod pocztowy w formacie 00-000.")
        String postalCode,

        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        BigDecimal longitude,

        Boolean hideExactAddress,

        @Size(max = 7)
        String terytSimc,

        @Size(max = 5)
        String terytUlic
) {
}
