-- Terminy umowne transakcji.
--
-- To nie są spotkania, tylko daty wynikające z podpisanych umów: koniec umowy
-- pośrednictwa, ważność rezerwacji, termin aktu. Agent ich nie umawia — ma ich
-- pilnować. Dlatego żyją przy transakcji, a kalendarz tylko je pokazuje
-- (odczytem z tej tabeli), zamiast trzymać kopię jako zwykły termin. Zmiana
-- daty na karcie zmienia ją w kalendarzu, bez dwóch źródeł prawdy.
--
-- Data, nie znacznik czasu: umowa mówi „do 30 listopada", a nie o godzinie
-- w UTC. TIMESTAMPTZ przesuwałby taki termin o dzień przy złej strefie.
--
-- Przesunięcie terminu (np. aneks do przedwstępnej) nie nadpisuje daty:
-- stary wiersz dostaje status MOVED, a nowy wskazuje na niego przez
-- moved_from_id. Historia aneksów zostaje na karcie.

CREATE TABLE deal_deadlines
(
    id            UUID        NOT NULL,
    deal_id       UUID        NOT NULL,
    type          VARCHAR(30) NOT NULL,
    due_date      DATE        NOT NULL,
    status        VARCHAR(20) NOT NULL,
    note          TEXT,
    moved_from_id UUID,
    resolved_at   TIMESTAMP(6) WITH TIME ZONE,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_deal_deadlines PRIMARY KEY (id),
    CONSTRAINT fk_deal_deadlines_deal FOREIGN KEY (deal_id) REFERENCES deals (id) ON DELETE CASCADE,
    CONSTRAINT fk_deal_deadlines_moved_from
        FOREIGN KEY (moved_from_id) REFERENCES deal_deadlines (id) ON DELETE SET NULL,
    CONSTRAINT ck_deal_deadlines_type CHECK (type IN ('MANDATE_END', 'EXCLUSIVITY_END', 'RESERVATION_END',
                                                      'FINAL_CONTRACT', 'MORTGAGE_DECISION', 'NOTARY_DEED',
                                                      'HANDOVER', 'OTHER')),
    CONSTRAINT ck_deal_deadlines_status CHECK (status IN ('OPEN', 'MET', 'MOVED')),
    CONSTRAINT ck_deal_deadlines_resolved CHECK ((status = 'OPEN') = (resolved_at IS NULL))
);

CREATE INDEX ix_deal_deadlines_deal ON deal_deadlines (deal_id, due_date);
-- Kalendarz pyta o zakres dat; tablica o najbliższy otwarty termin karty.
CREATE INDEX ix_deal_deadlines_due ON deal_deadlines (due_date, status);
