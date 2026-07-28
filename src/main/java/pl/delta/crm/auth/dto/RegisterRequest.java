package pl.delta.crm.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Podaj imię.")
        @Size(max = 80)
        String firstName,

        @NotBlank(message = "Podaj nazwisko.")
        @Size(max = 80)
        String lastName,

        @NotBlank(message = "Podaj adres e-mail.")
        @Email(message = "To nie wygląda na poprawny adres e-mail.")
        @Size(max = 190)
        String email,

        @NotBlank(message = "Podaj hasło.")
        @Size(min = 8, max = 100, message = "Hasło musi mieć co najmniej 8 znaków.")
        String password,

        @NotBlank(message = "Podaj nazwę biura.")
        @Size(max = 150)
        String agencyName
) {
}
