package pl.delta.crm.calendar.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.delta.crm.calendar.dictionary.EventOutcome;
import pl.delta.crm.calendar.dictionary.EventStatus;

/**
 * Domknięcie terminu jednym ruchem — osobno od pełnej edycji.
 *
 * <p>Agent oznacza wynik prezentacji zaraz po niej, zwykle z telefonu i między
 * jednym spotkaniem a drugim. Gdyby wymagało to otwarcia całego formularza,
 * rezultaty przestałyby być uzupełniane, a to one niosą tu całą wartość
 * analityczną (patrz {@link EventOutcome}).
 */
public record UpdateEventStatusRequest(

        @NotNull(message = "Wybierz status terminu.")
        EventStatus status,

        EventOutcome outcome,

        @Size(max = 5_000, message = "Notatka jest zbyt długa.")
        String outcomeNote
) {
}
