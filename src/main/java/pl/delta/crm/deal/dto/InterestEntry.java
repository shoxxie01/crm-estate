package pl.delta.crm.deal.dto;

import pl.delta.crm.deal.DealInterest;
import pl.delta.crm.deal.dictionary.InterestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Wiersz listy „Zainteresowani" w panelu karty i uczestnik terminu. */
public record InterestEntry(
        UUID id,
        UUID clientId,
        /** Nazwa do wyświetlenia. Z karty klienta albo wpisana ręcznie. */
        String name,
        String phone,
        InterestStatus status,
        BigDecimal offerAmount,
        String note,
        Instant createdAt
) {

    public static InterestEntry from(DealInterest interest) {
        return new InterestEntry(
                interest.getId(),
                interest.getClient() == null ? null : interest.getClient().getId(),
                interest.displayName(),
                interest.displayPhone(),
                interest.getStatus(),
                interest.getOfferAmount(),
                interest.getNote(),
                interest.getCreatedAt()
        );
    }
}
