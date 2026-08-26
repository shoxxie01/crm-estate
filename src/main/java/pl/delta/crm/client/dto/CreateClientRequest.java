package pl.delta.crm.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pl.delta.crm.client.dictionary.ClientStatus;
import pl.delta.crm.client.dictionary.LeadSource;

import java.util.UUID;

/**
 * Formularz nowego klienta. Wymagane są tylko imię i nazwisko — reguła
 * „telefon albo e-mail" dotyczy dwóch pól naraz, więc pilnuje jej serwis,
 * nie adnotacja na pojedynczym polu.
 */
public record CreateClientRequest(

        @NotBlank(message = "Podaj imię.")
        @Size(max = 80, message = "Imię jest zbyt długie.")
        String firstName,

        @NotBlank(message = "Podaj nazwisko.")
        @Size(max = 80, message = "Nazwisko jest zbyt długie.")
        String lastName,

        @Pattern(
                regexp = "^\\+?\\d(?:[ -]?\\d){8,14}$",
                message = "Podaj numer telefonu, np. +48 605 405 932.")
        @Size(max = 30, message = "Numer telefonu jest zbyt długi.")
        String phone,

        @Email(message = "Podaj poprawny adres e-mail.")
        @Size(max = 190, message = "Adres e-mail jest zbyt długi.")
        String email,

        LeadSource source,

        ClientStatus status,

        @Size(max = 5_000, message = "Notatka jest zbyt długa.")
        String notes,

        /** Opiekun kontaktu. Pusty = osoba dodająca klienta. */
        UUID agentId
) {
}
