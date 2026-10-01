package pl.delta.crm.deal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.delta.crm.deal.dictionary.DealLostReason;
import pl.delta.crm.deal.dictionary.DealStage;

/**
 * Przeciągnięcie karty do innej kolumny. Powód przegranej jest wymagany tylko
 * przy {@link DealStage#LOST}. Tę zależność pilnuje serwis.
 */
public record ChangeStageRequest(

        @NotNull(message = "Wybierz etap.")
        DealStage stage,

        DealLostReason lostReason,

        @Size(max = 2_000, message = "Notatka jest zbyt długa.")
        String lostNote
) {
}
