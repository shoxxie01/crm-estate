package pl.delta.crm.deal;

import pl.delta.crm.calendar.dictionary.EventOutcome;
import pl.delta.crm.calendar.dictionary.EventType;
import pl.delta.crm.deal.dictionary.DealStage;

import java.util.Optional;

/**
 * Podpowiada, do którego etapu przenieść transakcję po odbytym terminie.
 *
 * <p>To <b>propozycja</b>, nie automat: agent widzi ją w panelu terminu
 * i przenosi kartę jednym kliknięciem albo ją ignoruje. Automatyczne
 * przesuwanie kart po każdym rezultacie robiłoby z tablicy coś, czemu agent
 * przestaje ufać. Wystarczy jedna źle zamknięta prezentacja.
 *
 * <p>Reguły działają tylko w przód. Rezultat w rodzaju „brak zainteresowania"
 * niczego nie podpowiada: jeden niezainteresowany kupujący nie zamyka
 * sprzedaży, a o przegranej decyduje człowiek, podając powód.
 */
public final class DealStageAdvisor {

    /** Proponowany etap i jednozdaniowe uzasadnienie do pokazania agentowi. */
    public record Advice(DealStage stage, String reason) {
    }

    private DealStageAdvisor() {
    }

    public static Optional<Advice> advise(EventType type, EventOutcome outcome, DealStage current) {
        if (current.isClosed() || outcome == EventOutcome.NOT_INTERESTED) {
            return Optional.empty();
        }

        Advice advice = rule(type, outcome, current);
        // Tylko w przód. Propozycja cofnięcia karty byłaby myląca.
        if (advice == null || advice.stage().ordinal() <= current.ordinal()) {
            return Optional.empty();
        }
        return Optional.of(advice);
    }

    private static Advice rule(EventType type, EventOutcome outcome, DealStage current) {
        if (outcome == EventOutcome.OFFER_MADE) {
            return new Advice(DealStage.NEGOTIATION, "Kupujący złożył ofertę. Czas na negocjacje.");
        }

        if (outcome == EventOutcome.CONTRACT_SIGNED) {
            // Ta sama „umowa podpisana" znaczy co innego na różnych etapach:
            // przed sprzedażą to umowa pośrednictwa, w negocjacjach. Rezerwacja
            // albo przedwstępna, na końcu. Akt notarialny.
            return switch (current) {
                case LEAD, VALUATION -> new Advice(DealStage.MANDATE,
                        "Podpisana umowa pośrednictwa.");
                case MARKETING, NEGOTIATION -> new Advice(DealStage.RESERVATION,
                        "Podpisana umowa z kupującym. Rezerwacyjna albo przedwstępna.");
                case RESERVATION, CLOSING -> new Advice(DealStage.WON,
                        "Podpisana umowa końcowa. Transakcja zamknięta.");
                default -> null;
            };
        }

        return switch (type) {
            // Pierwsze spotkanie albo wycena się odbyły. Lead jest już rozmową.
            case MEETING, VALUATION -> current == DealStage.LEAD
                    ? new Advice(DealStage.VALUATION, "Pierwsze spotkanie z właścicielem się odbyło.")
                    : null;
            // Zdjęcia albo pierwsze pokazy oznaczają, że oferta jest już na rynku.
            case PHOTO_SESSION, PRESENTATION, OPEN_HOUSE -> current == DealStage.MANDATE
                    ? new Advice(DealStage.MARKETING, "Oferta jest już pokazywana. Aktywna sprzedaż.")
                    : null;
            default -> null;
        };
    }
}
