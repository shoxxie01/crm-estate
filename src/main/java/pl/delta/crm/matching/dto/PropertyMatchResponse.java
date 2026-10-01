package pl.delta.crm.matching.dto;

import pl.delta.crm.matching.CriterionResult;
import pl.delta.crm.property.dto.PropertySummary;

import java.util.List;

/** Oferta pasująca do poszukiwania. Na kartę klienta. */
public record PropertyMatchResponse(
        PropertySummary property,
        List<CriterionResult> criteria,
        long warnings
) {
}
