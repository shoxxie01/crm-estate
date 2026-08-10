package pl.delta.crm.property.dto;

import pl.delta.crm.property.dictionary.Flooring;
import pl.delta.crm.property.dictionary.HallStructure;
import pl.delta.crm.property.dictionary.ParkingType;

public record CommercialRequest(

        HallStructure structure,
        Flooring flooring,
        ParkingType parkingType,
        Boolean officeSpace,
        Boolean socialFacilities,
        Boolean loadingRamp
) {
}
