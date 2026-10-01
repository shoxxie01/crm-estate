package pl.delta.crm.deal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.delta.crm.calendar.dictionary.EventOutcome;
import pl.delta.crm.calendar.dictionary.EventType;
import pl.delta.crm.deal.dictionary.DealStage;

import static org.assertj.core.api.Assertions.assertThat;

/** Reguły podpowiedzi etapu. Czysta logika, bez Springa i bazy. */
class DealStageAdvisorTest {

    private static DealStage advise(EventType type, EventOutcome outcome, DealStage current) {
        return DealStageAdvisor.advise(type, outcome, current)
                .map(DealStageAdvisor.Advice::stage)
                .orElse(null);
    }

    @Test
    @DisplayName("złożona oferta prowadzi do negocjacji")
    void offerLeadsToNegotiation() {
        assertThat(advise(EventType.PRESENTATION, EventOutcome.OFFER_MADE, DealStage.MARKETING))
                .isEqualTo(DealStage.NEGOTIATION);
    }

    @Test
    @DisplayName("„umowa podpisana” znaczy co innego na każdym etapie")
    void contractSignedDependsOnStage() {
        assertThat(advise(EventType.CONTRACT_SIGNING, EventOutcome.CONTRACT_SIGNED, DealStage.VALUATION))
                .isEqualTo(DealStage.MANDATE);
        assertThat(advise(EventType.CONTRACT_SIGNING, EventOutcome.CONTRACT_SIGNED, DealStage.NEGOTIATION))
                .isEqualTo(DealStage.RESERVATION);
        assertThat(advise(EventType.CONTRACT_SIGNING, EventOutcome.CONTRACT_SIGNED, DealStage.CLOSING))
                .isEqualTo(DealStage.WON);
    }

    @Test
    @DisplayName("pierwsza prezentacja po umowie pośrednictwa to aktywna sprzedaż")
    void firstPresentationStartsMarketing() {
        assertThat(advise(EventType.PRESENTATION, EventOutcome.INTERESTED, DealStage.MANDATE))
                .isEqualTo(DealStage.MARKETING);
        assertThat(advise(EventType.PHOTO_SESSION, null, DealStage.MANDATE))
                .isEqualTo(DealStage.MARKETING);
    }

    @Test
    @DisplayName("nigdy nie cofa karty i nie rusza zamkniętych")
    void onlyForward() {
        // Oferta złożona, ale karta jest już w rezerwacji. Nie wracamy do negocjacji.
        assertThat(advise(EventType.PRESENTATION, EventOutcome.OFFER_MADE, DealStage.RESERVATION)).isNull();
        assertThat(advise(EventType.PRESENTATION, EventOutcome.OFFER_MADE, DealStage.WON)).isNull();
        assertThat(advise(EventType.PRESENTATION, EventOutcome.OFFER_MADE, DealStage.LOST)).isNull();
    }

    @Test
    @DisplayName("brak zainteresowania niczego nie podpowiada")
    void notInterestedSuggestsNothing() {
        assertThat(advise(EventType.PRESENTATION, EventOutcome.NOT_INTERESTED, DealStage.MANDATE)).isNull();
        assertThat(advise(EventType.PHONE_CALL, EventOutcome.INTERESTED, DealStage.MARKETING)).isNull();
    }
}
