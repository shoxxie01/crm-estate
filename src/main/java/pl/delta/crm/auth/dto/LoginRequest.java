package pl.delta.crm.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Podaj adres e-mail.")
        @Email(message = "To nie wygląda na poprawny adres e-mail.")
        String email,

        @NotBlank(message = "Podaj hasło.")
        String password
) {
}
