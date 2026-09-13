package pl.delta.crm.inquiry.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pl.delta.crm.client.requirement.dto.RequirementRequest;
import pl.delta.crm.inquiry.InquiryIntent;

/**
 * Zgłoszenie wysłane z publicznego formularza: kupno (kryteria w kształcie
 * formularza poszukiwania agenta — te same pola i reguły) albo sprzedaż
 * (opis nieruchomości).
 */
public record PublicInquiryRequest(

        @NotBlank(message = "Podaj imię.")
        @Size(max = 80, message = "Imię jest zbyt długie.")
        String firstName,

        @NotBlank(message = "Podaj nazwisko.")
        @Size(max = 80, message = "Nazwisko jest zbyt długie.")
        String lastName,

        @Pattern(
                regexp = "^\\+?\\d(?:[ -]?\\d){8,14}$",
                message = "Podaj numer telefonu, np. 605 405 932.")
        @Size(max = 30, message = "Numer telefonu jest zbyt długi.")
        String phone,

        @Email(message = "Podaj poprawny adres e-mail.")
        @Size(max = 190, message = "Adres e-mail jest zbyt długi.")
        String email,

        /** Pusta = kupno — tak działał formularz, zanim doszła sprzedaż. */
        InquiryIntent intent,

        /** Wymagane przy kupnie; przy sprzedaży musi być puste. Pilnuje serwis. */
        @Valid
        RequirementRequest criteria,

        /** Wymagane przy sprzedaży; przy kupnie musi być puste. Pilnuje serwis. */
        @Valid
        SaleOfferRequest offer,

        @Size(max = 2_000, message = "Wiadomość jest zbyt długa.")
        String message,

        // Obiekt, a nie boolean: brak pola ma być błędem walidacji przy tym polu,
        // a nie nieczytelnym „nie udało się odczytać danych" z deserializacji.
        @NotNull(message = "Bez tej zgody biuro nie może odpowiedzieć na zgłoszenie.")
        @AssertTrue(message = "Bez tej zgody biuro nie może odpowiedzieć na zgłoszenie.")
        Boolean consentProcessing,

        Boolean consentMarketing,

        /**
         * Pole-pułapka. Człowiek go nie widzi, więc zostawia puste; boty
         * wypełniają wszystko, co znajdą w formularzu.
         */
        String website
) {
}
