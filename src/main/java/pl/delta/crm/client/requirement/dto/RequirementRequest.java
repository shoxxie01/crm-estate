package pl.delta.crm.client.requirement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.delta.crm.client.dictionary.Financing;
import pl.delta.crm.client.dictionary.RequirementStatus;
import pl.delta.crm.property.dictionary.Feature;
import pl.delta.crm.property.dictionary.MarketType;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Formularz poszukiwania. Wymagane są tylko transakcja i rodzaj nieruchomości —
 * reszta to tyle, ile klient zdążył powiedzieć. Relacje między polami
 * („od" nie większe niż „do", pokój tylko na wynajem) sprawdza serwis.
 */
public record RequirementRequest(

        @NotNull(message = "Wybierz, czy klient chce kupić, czy najmować.")
        TransactionType transactionType,

        /** Pusty przy tworzeniu = aktywne; przy edycji = bez zmian. */
        RequirementStatus status,

        @NotEmpty(message = "Wybierz przynajmniej jeden rodzaj nieruchomości.")
        Set<PropertyType> propertyTypes,

        /** Pusty = rynek obojętny. */
        MarketType marketType,

        @Valid
        @Size(max = 20, message = "Podaj maksymalnie 20 lokalizacji.")
        List<LocationRequest> locations,

        @DecimalMin(value = "0", message = "Kwota nie może być ujemna.")
        @Digits(integer = 12, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal priceMin,

        @DecimalMin(value = "0", message = "Kwota nie może być ujemna.")
        @Digits(integer = 12, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal priceMax,

        @DecimalMin(value = "0", message = "Powierzchnia nie może być ujemna.")
        @Digits(integer = 8, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal areaMin,

        @DecimalMin(value = "0", message = "Powierzchnia nie może być ujemna.")
        @Digits(integer = 8, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal areaMax,

        @Min(value = 1, message = "Liczba pokoi musi być dodatnia.")
        @Max(value = 100, message = "Liczba pokoi jest zbyt duża.")
        Short roomsMin,

        @Min(value = 1, message = "Liczba pokoi musi być dodatnia.")
        @Max(value = 100, message = "Liczba pokoi jest zbyt duża.")
        Short roomsMax,

        @Min(value = -1, message = "Najniższa dopuszczalna wartość to -1 (suterena).")
        @Max(value = 160, message = "Piętro nie może być wyższe niż 160.")
        Short floorMin,

        @Min(value = -1, message = "Najniższa dopuszczalna wartość to -1 (suterena).")
        @Max(value = 160, message = "Piętro nie może być wyższe niż 160.")
        Short floorMax,

        Boolean excludeTopFloor,

        /** Tylko przy kupnie — przy najmie serwis go czyści. */
        Financing financing,

        LocalDate moveInDate,

        Set<Feature> requiredFeatures,

        Set<Feature> preferredFeatures,

        @Size(max = 5_000, message = "Notatka jest zbyt długa.")
        String notes
) {

    /**
     * Kryteria z publicznego formularza w postaci do zapisania: bez stanu (nowe
     * poszukiwanie zawsze jest aktywne) i z wiadomością klienta jako notatką.
     */
    public RequirementRequest forInquiry(String message) {
        return new RequirementRequest(transactionType, null, propertyTypes, marketType, locations,
                priceMin, priceMax, areaMin, areaMax, roomsMin, roomsMax, floorMin, floorMax,
                excludeTopFloor, financing, moveInDate, requiredFeatures, preferredFeatures, message);
    }

    public record LocationRequest(
            @NotBlank(message = "Podaj miejscowość.")
            @Size(max = 80, message = "Nazwa miejscowości jest zbyt długa.")
            String city,

            @Size(max = 64, message = "Nazwa dzielnicy jest zbyt długa.")
            String district
    ) {
    }
}
