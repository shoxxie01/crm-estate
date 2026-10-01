package pl.delta.crm.deal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import pl.delta.crm.deal.dictionary.DealStage;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Formularz transakcji. Ten sam przy dodawaniu i edycji.
 *
 * <p>Nic nie jest wymagane: przy pierwszym telefonie agent często nie ma nawet
 * nazwiska, a kartę i tak chce mieć na tablicy. Pusty tytuł serwis składa
 * z adresu oferty albo nazwiska właściciela, tak jak tytuł terminu.
 *
 * <p>Kupującego tu nie ma. Wynika z przyjętej oferty na liście
 * zainteresowanych ({@code /api/deals/{id}/interests}).
 *
 * <p>{@code stage} jest brany pod uwagę tylko przy dodawaniu. Zmiana etapu
 * istniejącej karty idzie przez {@code PUT /api/deals/{id}/stage}, bo zapisuje
 * historię i wymaga powodu przegranej. Edycja pól nie powinna tego omijać.
 */
public record DealRequest(

        @Size(max = 160, message = "Tytuł jest zbyt długi.")
        String title,

        DealStage stage,

        /** Prowadzący agent. Pusty = osoba dodająca (przy edycji: bez zmian). */
        UUID agentId,

        /** Właściciel. Strona podaży. */
        UUID clientId,

        UUID propertyId,

        @DecimalMin(value = "0", message = "Wartość nie może być ujemna.")
        @Digits(integer = 12, fraction = 2, message = "Podaj kwotę, np. 549000.")
        BigDecimal value,

        @DecimalMin(value = "0", message = "Prowizja nie może być ujemna.")
        @Digits(integer = 12, fraction = 2, message = "Podaj kwotę, np. 16470.")
        BigDecimal commission,

        @Size(max = 5_000, message = "Notatka jest zbyt długa.")
        String notes
) {
}
