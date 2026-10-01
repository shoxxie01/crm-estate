-- Zainteresowani ofertą w ramach jednej transakcji.
--
-- Karta Kanbana jest jedna na ofertę (V16), ale kupujących bywa kilku naraz:
-- dwie osoby oglądają mieszkanie tego samego dnia, jedna składa ofertę, druga
-- się zastanawia. Każda z nich ma tu własny wiersz i własny status — to mini-
-- lejek po stronie popytu wewnątrz karty, zamiast osobnych kart, które
-- liczyłyby wartość tej samej oferty kilka razy.
--
-- Zainteresowany jest klientem z bazy albo — gdy dzwoni ktoś z ogłoszenia
-- i umawia się na oglądanie — samym imieniem i telefonem. Zakładanie karty
-- klienta każdemu, kto raz obejrzy mieszkanie, zaśmieciłoby kartotekę.

CREATE TABLE deal_interests
(
    id           UUID          NOT NULL,
    deal_id      UUID          NOT NULL,

    client_id    UUID,
    name         VARCHAR(160),
    phone        VARCHAR(30),

    status       VARCHAR(20)   NOT NULL,
    -- Ostatnia kwota zaproponowana przez tę osobę. Historia negocjacji zostaje
    -- w notatkach i rezultatach terminów — tu liczy się stan bieżący.
    offer_amount NUMERIC(14, 2),
    note         TEXT,

    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_deal_interests PRIMARY KEY (id),
    CONSTRAINT fk_deal_interests_deal FOREIGN KEY (deal_id) REFERENCES deals (id) ON DELETE CASCADE,
    -- Usunięcie klienta (np. na żądanie RODO) usuwa też jego zainteresowanie.
    CONSTRAINT fk_deal_interests_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE,
    CONSTRAINT ck_deal_interests_who CHECK (client_id IS NOT NULL OR name IS NOT NULL),
    CONSTRAINT ck_deal_interests_status CHECK (status IN ('NEW', 'VIEWED', 'CONSIDERING', 'OFFER', 'ACCEPTED', 'DROPPED')),
    CONSTRAINT ck_deal_interests_offer CHECK (offer_amount IS NULL OR offer_amount >= 0)
);

CREATE INDEX ix_deal_interests_deal ON deal_interests (deal_id);
CREATE INDEX ix_deal_interests_client ON deal_interests (client_id);

-- Ten sam klient nie może być zainteresowany tą samą transakcją dwa razy.
CREATE UNIQUE INDEX uq_deal_interests_client ON deal_interests (deal_id, client_id)
    WHERE client_id IS NOT NULL;

-- Przyjęta może być tylko jedna oferta — to ona wyznacza kupującego karty.
CREATE UNIQUE INDEX uq_deal_interests_accepted ON deal_interests (deal_id)
    WHERE status = 'ACCEPTED';

-- Uczestnicy terminu. Wspólne oglądanie dwóch osób to jeden termin w kalendarzu
-- agenta, ale dwa osobne wiersze tutaj — każdy z zainteresowanych ma potem
-- własny status, niezależny od rezultatu całego terminu.
CREATE TABLE calendar_event_participants
(
    event_id    UUID NOT NULL,
    interest_id UUID NOT NULL,

    CONSTRAINT pk_calendar_event_participants PRIMARY KEY (event_id, interest_id),
    CONSTRAINT fk_calendar_event_participants_event
        FOREIGN KEY (event_id) REFERENCES calendar_events (id) ON DELETE CASCADE,
    CONSTRAINT fk_calendar_event_participants_interest
        FOREIGN KEY (interest_id) REFERENCES deal_interests (id) ON DELETE CASCADE
);

CREATE INDEX ix_calendar_event_participants_interest ON calendar_event_participants (interest_id);
