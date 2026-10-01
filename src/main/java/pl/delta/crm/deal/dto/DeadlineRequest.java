package pl.delta.crm.deal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.delta.crm.deal.dictionary.DeadlineType;

import java.time.LocalDate;

/**
 * Termin umowny. Dodanie, poprawka albo przesunięcie (aneks). Przy
 * przesunięciu {@code type} jest ignorowany: rodzaj zostaje ten sam.
 */
public record DeadlineRequest(

        DeadlineType type,

        @NotNull(message = "Podaj datę terminu.")
        LocalDate dueDate,

        @Size(max = 2_000, message = "Notatka jest zbyt długa.")
        String note
) {
}
