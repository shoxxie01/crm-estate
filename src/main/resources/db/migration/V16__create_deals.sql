-- Transakcje — karty tablicy Kanban (lejek sprzedaży biura).
--
-- Transakcja to jedno zlecenie po stronie podaży: od pierwszego kontaktu
-- z właścicielem, przez umowę pośrednictwa i prezentacje, do aktu notarialnego
-- albo umowy najmu. Wszystkie powiązania są opcjonalne z tego samego powodu co
-- w kalendarzu (V10): lead powstaje, zanim jest oferta, a często zanim jest
-- nawet karta klienta.
--
-- Etap trzymamy jako kolumnę, a przejścia osobno w `deal_stage_changes`.
-- Sama kolumna wystarczy tablicy; historia jest po to, żeby dało się policzyć
-- konwersję między etapami i średni czas w etapie bez zgadywania z dat.

CREATE TABLE deals
(
    id                UUID          NOT NULL,
    agency_id         UUID          NOT NULL,

    -- Prowadzący agent i autor wpisu — jak przy ofertach i terminach.
    agent_id          UUID          NOT NULL,
    created_by        UUID          NOT NULL,

    title             VARCHAR(160)  NOT NULL,
    stage             VARCHAR(20)   NOT NULL,

    -- Właściciel (strona podaży), oferta i — od etapu negocjacji — kupujący
    -- lub najemca. Kupujący jest klientem z tej samej tabeli `clients`
    -- (decyzja z modułu poszukiwań, V13). Usunięcie klienta albo oferty nie
    -- kasuje transakcji, tylko odpina powiązanie: lejek i jego statystyki
    -- mają przetrwać porządki w kartotece.
    client_id         UUID,
    property_id       UUID,
    buyer_id          UUID,

    -- Oczekiwana cena transakcji i prowizja biura. Sumy tych kolumn w nagłówkach
    -- tablicy to „ile jest w lejku".
    deal_value        NUMERIC(14, 2),
    commission        NUMERIC(14, 2),

    notes             TEXT,

    -- Powód przegranej jest obowiązkowy (CHECK niżej) — bez niego kolumna
    -- „Przegrane" nie mówi nic poza tym, że coś poszło nie tak.
    lost_reason       VARCHAR(30),
    lost_note         TEXT,

    stage_changed_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    closed_at         TIMESTAMP(6) WITH TIME ZONE,
    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_deals PRIMARY KEY (id),
    CONSTRAINT fk_deals_agency FOREIGN KEY (agency_id) REFERENCES agencies (id),
    CONSTRAINT fk_deals_agent FOREIGN KEY (agent_id) REFERENCES users (id),
    CONSTRAINT fk_deals_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_deals_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE SET NULL,
    CONSTRAINT fk_deals_property FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE SET NULL,
    CONSTRAINT fk_deals_buyer FOREIGN KEY (buyer_id) REFERENCES clients (id) ON DELETE SET NULL,
    CONSTRAINT ck_deals_stage CHECK (stage IN ('LEAD', 'VALUATION', 'MANDATE', 'MARKETING',
                                               'NEGOTIATION', 'RESERVATION', 'CLOSING', 'WON', 'LOST')),
    CONSTRAINT ck_deals_lost CHECK ((stage = 'LOST') = (lost_reason IS NOT NULL)),
    CONSTRAINT ck_deals_closed CHECK ((stage IN ('WON', 'LOST')) = (closed_at IS NOT NULL)),
    CONSTRAINT ck_deals_value CHECK (deal_value IS NULL OR deal_value >= 0),
    CONSTRAINT ck_deals_commission CHECK (commission IS NULL OR commission >= 0)
);

-- Jedna otwarta transakcja na ofertę. Dwie karty tej samej oferty w lejku to
-- podwójnie liczona wartość i dwa źródła prawdy o tym, na jakim etapie jest
-- sprzedaż. Zamknięte (wygrane/przegrane) mogą się powtarzać — oferta wraca
-- na rynek po zerwanej umowie przedwstępnej.
CREATE UNIQUE INDEX uq_deals_open_property ON deals (property_id)
    WHERE property_id IS NOT NULL AND stage NOT IN ('WON', 'LOST');

CREATE INDEX ix_deals_agency_stage ON deals (agency_id, stage);
CREATE INDEX ix_deals_client ON deals (client_id);
CREATE INDEX ix_deals_buyer ON deals (buyer_id);

CREATE TABLE deal_stage_changes
(
    id          UUID        NOT NULL,
    deal_id     UUID        NOT NULL,
    -- Pusty przy założeniu karty — pierwszy wpis historii to „powstała w etapie X".
    from_stage  VARCHAR(20),
    to_stage    VARCHAR(20) NOT NULL,
    changed_by  UUID        NOT NULL,
    changed_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_deal_stage_changes PRIMARY KEY (id),
    CONSTRAINT fk_deal_stage_changes_deal FOREIGN KEY (deal_id) REFERENCES deals (id) ON DELETE CASCADE,
    CONSTRAINT fk_deal_stage_changes_user FOREIGN KEY (changed_by) REFERENCES users (id)
);

CREATE INDEX ix_deal_stage_changes_deal ON deal_stage_changes (deal_id, changed_at);

-- Termin może dotyczyć transakcji. Na karcie Kanbana widać najbliższy z nich,
-- a w panelu karty — całą historię. Usunięcie karty zostawia terminy
-- w kalendarzu, tak jak usunięcie oferty (patrz CalendarEventRepository).
ALTER TABLE calendar_events ADD COLUMN deal_id UUID;
ALTER TABLE calendar_events
    ADD CONSTRAINT fk_calendar_events_deal FOREIGN KEY (deal_id) REFERENCES deals (id) ON DELETE SET NULL;
CREATE INDEX ix_calendar_events_deal ON calendar_events (deal_id, starts_at);
