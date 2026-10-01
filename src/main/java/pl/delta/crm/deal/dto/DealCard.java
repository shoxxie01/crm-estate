package pl.delta.crm.deal.dto;

import pl.delta.crm.calendar.dto.EventSummary;
import pl.delta.crm.client.Client;
import pl.delta.crm.deal.Deal;
import pl.delta.crm.deal.dictionary.DealLostReason;
import pl.delta.crm.deal.dictionary.DealStage;
import pl.delta.crm.property.Property;
import pl.delta.crm.property.dictionary.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Karta na tablicy Kanban. Płaska, jak {@link EventSummary}, bo tablica
 * renderuje kilkadziesiąt takich naraz.
 *
 * <p>{@code nextEvent} to najbliższy aktywny termin powiązany z transakcją.
 * Pusty przy otwartej karcie znaczy „nikt nie zaplanował kolejnego kroku".
 * Tablica pokazuje to jako ostrzeżenie, bo właśnie tak giną transakcje.
 *
 * <p>Liczniki zainteresowanych mówią, ile osób jest jeszcze w grze i ile ma
 * na stole konkretną kwotę. „3 zainteresowanych · 1 oferta" to inna rozmowa
 * z właścicielem niż „0 ofert po miesiącu".
 */
public record DealCard(
        UUID id,
        String title,
        DealStage stage,
        Instant stageChangedAt,
        Instant closedAt,
        BigDecimal value,
        BigDecimal commission,

        UUID agentId,
        String agentName,

        UUID clientId,
        String clientName,

        /** Kupujący z bazy klientów. Pusty, gdy przyjęta oferta jest od osoby spoza bazy. */
        UUID buyerId,
        /** Nazwa kupującego. Z bazy albo z przyjętej oferty osoby spoza bazy. */
        String buyerName,

        UUID propertyId,
        String propertyReference,
        String propertyAddress,
        TransactionType transactionType,

        DealLostReason lostReason,
        EventSummary nextEvent,

        int interestCount,
        int offerCount,
        BigDecimal bestOffer,

        /** Najbliższy otwarty termin umowny. Z niego plakietka „wygasa za N dni". */
        DeadlineEntry nextDeadline,

        Instant createdAt
) {

    /** Podsumowanie listy zainteresowanych jednej karty. */
    public record InterestStats(int active, int offers, BigDecimal bestOffer, String acceptedName) {
    }

    public static DealCard from(Deal deal, EventSummary nextEvent, InterestStats stats,
                                DeadlineEntry nextDeadline) {
        Client client = deal.getClient();
        Client buyer = deal.getBuyer();
        Property property = deal.getProperty();

        return new DealCard(
                deal.getId(),
                deal.getTitle(),
                deal.getStage(),
                deal.getStageChangedAt(),
                deal.getClosedAt(),
                deal.getValue(),
                deal.getCommission(),
                deal.getAgent().getId(),
                deal.getAgent().getFirstName() + " " + deal.getAgent().getLastName(),
                client == null ? null : client.getId(),
                client == null ? null : client.fullName(),
                buyer == null ? null : buyer.getId(),
                buyer != null ? buyer.fullName() : stats.acceptedName(),
                property == null ? null : property.getId(),
                property == null ? null : property.getReferenceNumber(),
                property == null || property.getAddress() == null ? null : property.getAddress().shortLine(),
                property == null ? null : property.getTransactionType(),
                deal.getLostReason(),
                nextEvent,
                stats.active(),
                stats.offers(),
                stats.bestOffer(),
                nextDeadline,
                deal.getCreatedAt()
        );
    }
}
