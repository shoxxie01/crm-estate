package pl.delta.crm.matching.dto;

import pl.delta.crm.matching.CriterionResult;
import pl.delta.crm.property.dto.PropertySummary;

import java.util.List;

/** Oferta pasująca do poszukiwania — na kartę klienta. */
public record PropertyMatchResponse(
        PropertySummary property,
        List<CriterionResult> criteria,
        long warnings
) {
}
