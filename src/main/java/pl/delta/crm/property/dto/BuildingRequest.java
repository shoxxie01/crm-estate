package pl.delta.crm.property.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import pl.delta.crm.property.dictionary.BuildingMaterial;
import pl.delta.crm.property.dictionary.BuildingType;
import pl.delta.crm.property.dictionary.ConstructionStatus;
import pl.delta.crm.property.dictionary.GarretType;
import pl.delta.crm.property.dictionary.OwnershipForm;
import pl.delta.crm.property.dictionary.RoofType;
import pl.delta.crm.property.dictionary.Roofing;
import pl.delta.crm.property.dictionary.Surroundings;
import pl.delta.crm.property.dictionary.WindowsType;

public record BuildingRequest(

        @Min(value = 1500, message = "Rok budowy wygląda na nieprawidłowy.")
        @Max(value = 2100)
        Short buildYear,

        BuildingType buildingType,
        BuildingMaterial buildingMaterial,
        ConstructionStatus constructionStatus,
        WindowsType windowsType,
        RoofType roofType,
        Roofing roofing,
        GarretType garretType,
        OwnershipForm ownershipForm,
        Surroundings surroundings,
        Boolean furnished
) {
}
