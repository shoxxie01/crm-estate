package pl.delta.crm.property.dto;

import jakarta.validation.constraints.Size;

/** Podpis pod zdjęciem. Pusty kasuje istniejący. */
public record UpdateCaptionRequest(
        @Size(max = 150, message = "Podpis może mieć najwyżej 150 znaków.")
        String caption
) {
}
