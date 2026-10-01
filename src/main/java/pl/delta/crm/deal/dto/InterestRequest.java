package pl.delta.crm.deal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pl.delta.crm.deal.dictionary.InterestStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Zainteresowany. Klient z bazy ({@code clientId}) albo imię i telefon.
 * Jedno z dwóch jest wymagane; tę zależność pilnuje serwis.
 */
public record InterestRequest(

        UUID clientId,

        @Size(max = 160, message = "Imię i nazwisko jest zbyt długie.")
        String name,

        // Ten sam format co przy kliencie i drugiej stronie terminu.
        @Pattern(
                regexp = "^\\+?\\d(?:[ -]?\\d){8,14}$",
                message = "Podaj numer telefonu, np. 605 405 932.")
        @Size(max = 30, message = "Numer telefonu jest zbyt długi.")
        String phone,

        /** Pusty przy dodawaniu = „zgłosił się". */
        InterestStatus status,

        @DecimalMin(value = "0", message = "Kwota nie może być ujemna.")
        @Digits(integer = 12, fraction = 2, message = "Podaj kwotę, np. 530000.")
        BigDecimal offerAmount,

        @Size(max = 2_000, message = "Notatka jest zbyt długa.")
        String note
) {
}
