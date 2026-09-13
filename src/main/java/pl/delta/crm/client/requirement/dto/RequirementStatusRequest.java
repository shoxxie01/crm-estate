package pl.delta.crm.client.requirement.dto;

import jakarta.validation.constraints.NotNull;
import pl.delta.crm.client.dictionary.RequirementStatus;

/** Szybka zmiana stanu z karty klienta, bez przesyłania całego formularza. */
public record RequirementStatusRequest(
        @NotNull(message = "Wybierz stan poszukiwania.")
        RequirementStatus status
) {
}
