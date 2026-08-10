-- Klienci biura.
--
-- W tej wersji CRM-u klient to zawsze osoba prywatna po stronie podaży —
-- właściciel, który zleca biuru sprzedaż albo wynajem swojej nieruchomości.
-- Świadomie NIE ma tu kolumny „typ klienta" (sprzedający/wynajmujący): to
-- rozróżnienie wynika z transaction_type ofert powierzonych przez klienta
-- (properties.owner_client_id, migracja V7), a nie z cechy osoby. Jeden
-- właściciel może jedną nieruchomość sprzedawać, a inną wynajmować.

CREATE TABLE clients
(
    id          UUID         NOT NULL,
    agency_id   UUID         NOT NULL,

    -- Opiekun kontaktu i autor wpisu — jak agent/created_by przy ofertach.
    agent_id    UUID         NOT NULL,
    created_by  UUID         NOT NULL,

    first_name  VARCHAR(80)  NOT NULL,
    last_name   VARCHAR(80)  NOT NULL,

    -- Telefon jest podstawowym kanałem na rynku nieruchomości, ale nie każdy
    -- kontakt przychodzi telefonicznie — stąd oba pola opcjonalne, a wymóg
    -- „przynajmniej jeden kanał" pilnuje CHECK poniżej.
    phone       VARCHAR(30),
    email       VARCHAR(190),

    -- Źródło pozyskania (polecenie, portal, telefon...). Wartości ze słownika
    -- LeadSource — trzymamy własne nazwy techniczne, nie kody zewnętrzne.
    source      VARCHAR(20),
    status      VARCHAR(20)  NOT NULL,
    notes       TEXT,

    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_clients PRIMARY KEY (id),
    CONSTRAINT fk_clients_agency FOREIGN KEY (agency_id) REFERENCES agencies (id),
    CONSTRAINT fk_clients_agent FOREIGN KEY (agent_id) REFERENCES users (id),
    CONSTRAINT fk_clients_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_clients_status CHECK (status IN ('AKTYWNY', 'ARCHIWALNY')),
    CONSTRAINT ck_clients_contact CHECK (phone IS NOT NULL OR email IS NOT NULL)
);

-- Lista klientów jest zawsze filtrowana po agencji i sortowana po dacie.
CREATE INDEX ix_clients_agency_created ON clients (agency_id, created_at DESC);
CREATE INDEX ix_clients_agency_status ON clients (agency_id, status);
CREATE INDEX ix_clients_agent ON clients (agent_id);
