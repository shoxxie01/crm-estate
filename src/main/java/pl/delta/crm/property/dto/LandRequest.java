package pl.delta.crm.property.dto;

import jakarta.validation.constraints.Size;
import pl.delta.crm.property.dictionary.PlotType;
import pl.delta.crm.property.dictionary.RoadAccess;

public record LandRequest(

        PlotType plotType,

        @Size(max = 32, message = "Wymiary mogą mieć najwyżej 32 znaki.")
        String dimensions,

        RoadAccess roadAccess,
        Boolean fenced,

        @Size(max = 200)
        String zoningPlan
) {
}
