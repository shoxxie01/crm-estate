-- Zgłoszenia z publicznego formularza „czego szukasz".
--
-- Formularz jest dostępny bez logowania, więc to, co przychodzi, nie trafia od
-- razu do `clients`: spam, pomyłki i duplikaty zaśmiecałyby bazę. Zgłoszenie
-- czeka w skrzynce, agent je przegląda i dopiero wtedy zamienia na klienta
-- z poszukiwaniem (albo dopina poszukiwanie do klienta, który już jest).
-- Odrzucone zgłoszenie jest usuwane, a nie archiwizowane — to dane osobowe
-- kogoś, z kim biuro nie nawiązało współpracy.

-- Link do formularza. Losowy, a nie np. nazwa biura: po wycieku do spamerów
-- administrator generuje nowy, a stary przestaje działać.
ALTER TABLE agencies ADD COLUMN intake_token VARCHAR(40);
UPDATE agencies SET intake_token = replace(gen_random_uuid()::text, '-', '');
ALTER TABLE agencies ALTER COLUMN intake_token SET NOT NULL;
ALTER TABLE agencies ADD CONSTRAINT uq_agencies_intake_token UNIQUE (intake_token);

CREATE TABLE client_inquiries
(
    id                    UUID         NOT NULL,
    agency_id             UUID         NOT NULL,
    status                VARCHAR(20)  NOT NULL,

    first_name            VARCHAR(80)  NOT NULL,
    last_name             VARCHAR(80)  NOT NULL,
    phone                 VARCHAR(30),
    email                 VARCHAR(190),

    -- Kryteria w kształcie formularza poszukiwania (JSON). Zgłoszenie jest
    -- tylko poczekalnią: nikt nie filtruje po tych polach w SQL, a przy
    -- przyjęciu i tak powstaje z nich prawdziwe `client_requirements`.
    criteria              TEXT         NOT NULL,
    message               TEXT,

    -- RODO: kiedy i na jaką dokładnie treść klauzuli klient się zgodził.
    -- Tekst, a nie numer wersji — dowód ma przetrwać zmianę treści w kodzie.
    consent_processing_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    consent_text          TEXT         NOT NULL,
    consent_marketing     BOOLEAN      NOT NULL,

    -- Wypełnione po przyjęciu zgłoszenia.
    client_id             UUID,
    handled_by            UUID,
    handled_at            TIMESTAMP(6) WITH TIME ZONE,

    created_at            TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_client_inquiries PRIMARY KEY (id),
    CONSTRAINT fk_client_inquiries_agency FOREIGN KEY (agency_id) REFERENCES agencies (id),
    -- Usunięcie klienta (np. na jego żądanie) usuwa też zgłoszenie z jego danymi.
    CONSTRAINT fk_client_inquiries_client
        FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE,
    CONSTRAINT fk_client_inquiries_handled_by FOREIGN KEY (handled_by) REFERENCES users (id),
    CONSTRAINT ck_client_inquiries_status CHECK (status IN ('NEW', 'CONVERTED')),
    CONSTRAINT ck_client_inquiries_contact CHECK (phone IS NOT NULL OR email IS NOT NULL),
    CONSTRAINT ck_client_inquiries_converted CHECK ((status = 'CONVERTED') = (client_id IS NOT NULL))
);

-- Skrzynka: zawsze w obrębie biura, po stanie, najnowsze na górze.
CREATE INDEX ix_client_inquiries_agency_status ON client_inquiries (agency_id, status, created_at DESC);
CREATE INDEX ix_client_inquiries_client ON client_inquiries (client_id);
