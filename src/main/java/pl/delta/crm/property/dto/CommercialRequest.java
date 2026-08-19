package pl.delta.crm.property.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import pl.delta.crm.property.dictionary.Flooring;
import pl.delta.crm.property.dictionary.HallStructure;
import pl.delta.crm.property.dictionary.ParkingType;

import java.math.BigDecimal;

public record CommercialRequest(

        HallStructure structure,
        Flooring flooring,
        ParkingType parkingType,
        Boolean officeSpace,
        Boolean socialFacilities,
        Boolean loadingRamp,

        /* Parametry hali / magazynu. */
        @DecimalMin(value = "0.00")
        @Digits(integer = 6, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal powerConnectionKw,

        @DecimalMin(value = "0.00")
        @Digits(integer = 4, fraction = 2, message = "Maksymalnie 2 miejsca po przecinku.")
        BigDecimal floorLoadPerM2,

        @Min(0) @Max(1000)
        Short loadingDocksCount
) {
}
