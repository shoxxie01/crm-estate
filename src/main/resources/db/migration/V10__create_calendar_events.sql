-- Kalendarz biura.
--
-- Wpis kalendarza prawie zawsze czegoś dotyczy: prezentacji konkretnej oferty,
-- wyceny, podpisania umowy z właścicielem. Dlatego zamiast osobnego terminarza
-- trzymamy zdarzenie z opcjonalnymi wiązaniami do oferty i do klienta —
-- opcjonalnymi, bo spotkanie akwizycyjne odbywa się ZANIM powstanie oferta,
-- a rekrutacyjne czy wewnętrzne nie dotyczy żadnej.
--
-- Druga strona spotkania (kupujący / najemca) jest tu polami tekstowymi, a nie
-- kluczem obcym. Powód: `clients` to w tej wersji CRM-u wyłącznie strona podaży
-- (właściciel powierzający ofertę — patrz V6). Kupujących w modelu nie ma, a
-- wpychanie ich do `clients` zepsułoby regułę, że rola klienta wynika z typu
-- transakcji jego ofert. Gdy powstanie moduł poszukujących, counterparty_*
-- zamieni się na FK bez ruszania reszty tabeli.
--
-- Czas trzymamy w TIMESTAMPTZ (jak created_at w pozostałych tabelach), a nie
-- w czasie lokalnym: inaczej przejście na czas letni przesuwałoby terminy już
-- zapisane w bazie.

CREATE TABLE calendar_events
(
    id                 UUID         NOT NULL,
    agency_id          UUID         NOT NULL,

    -- Czyj to termin i kto go wpisał — jak agent/created_by przy ofertach.
    agent_id           UUID         NOT NULL,
    created_by         UUID         NOT NULL,

    -- Kontekst zdarzenia. Oba opcjonalne i oba mogą wystąpić naraz
    -- (oględziny oferty z jej właścicielem).
    property_id        UUID,
    client_id          UUID,

    type               VARCHAR(30)  NOT NULL,
    status             VARCHAR(20)  NOT NULL,

    -- Rezultat ma sens dopiero po fakcie, stąd CHECK niżej: bez statusu
    -- ODBYLO_SIE zostaje pusty. To z niego bierze się realna wartość kalendarza
    -- dla biura — „trzy prezentacje, wszystkie: cena za wysoka" to sygnał do
    -- rozmowy z właścicielem, a nie wpis w terminarzu.
    outcome            VARCHAR(30),
    outcome_note       TEXT,

    title              VARCHAR(120) NOT NULL,
    description        TEXT,

    -- Miejsce podawane ręcznie — używane, gdy zdarzenie nie ma powiązanej
    -- oferty (spotkanie w biurze, notariusz). Przy powiązanej ofercie adres
    -- bierze się z niej i to pole zostaje puste.
    location           VARCHAR(200),

    starts_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    ends_at            TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    all_day            BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Druga strona spotkania spoza bazy klientów (kupujący, najemca, rzeczoznawca).
    counterparty_name  VARCHAR(160),
    counterparty_phone VARCHAR(30),

    created_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_calendar_events PRIMARY KEY (id),
    CONSTRAINT fk_calendar_events_agency FOREIGN KEY (agency_id) REFERENCES agencies (id),
    CONSTRAINT fk_calendar_events_agent FOREIGN KEY (agent_id) REFERENCES users (id),
    CONSTRAINT fk_calendar_events_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_calendar_events_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_calendar_events_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT ck_calendar_events_status
        CHECK (status IN ('PLANOWANE', 'POTWIERDZONE', 'ODBYLO_SIE', 'ODWOLANE', 'NIEOBECNOSC')),
    CONSTRAINT ck_calendar_events_range CHECK (ends_at > starts_at),
    CONSTRAINT ck_calendar_events_outcome CHECK (outcome IS NULL OR status = 'ODBYLO_SIE')
);

-- Kalendarz zawsze pyta o zakres dat w obrębie biura; widok „mój tydzień"
-- dokłada do tego agenta.
CREATE INDEX ix_calendar_events_agency_start ON calendar_events (agency_id, starts_at);
CREATE INDEX ix_calendar_events_agent_start ON calendar_events (agent_id, starts_at);

-- Sekcja „Terminy" na karcie oferty i karcie klienta.
CREATE INDEX ix_calendar_events_property ON calendar_events (property_id);
CREATE INDEX ix_calendar_events_client ON calendar_events (client_id);
