package pl.delta.crm.matching.dto;

import pl.delta.crm.client.requirement.dto.RequirementResponse;
import pl.delta.crm.matching.CriterionResult;

import java.util.List;
import java.util.UUID;

/**
 * Klient, którego poszukiwanie pasuje do oferty. Na kartę oferty. Kontakt jest
 * w odpowiedzi od razu, bo następny krok agenta to telefon do klienta.
 */
public record ClientMatchResponse(
        UUID clientId,
        String clientName,
        String phone,
        String email,
        String agentName,
        RequirementResponse requirement,
        List<CriterionResult> criteria,
        long warnings
) {
}
