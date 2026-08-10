-- Agencja jako byt, nie jako tekst w kolumnie users.agency_name.
--
-- Do tej pory nazwa biura była wolnym tekstem przy użytkowniku. Od momentu, w
-- którym pojawiają się dane wspólne dla całego biura (oferty, klienci, umowy),
-- potrzebny jest identyfikator, po którym można je filtrować — inaczej nie da
-- się odpowiedzieć na pytanie „kto widzi którą ofertę" inaczej niż przez
-- porównywanie stringów.

CREATE TABLE agencies
(
    id             UUID         NOT NULL,
    name           VARCHAR(150) NOT NULL,

    -- E-mail biura zarejestrowany w portalu. Otodom Import identyfikuje po nim
    -- nadawcę paczki (tag <Agency>), więc trzymamy go osobno od e-maili agentów.
    contact_email  VARCHAR(190),
    contact_phone  VARCHAR(30),

    -- Numer licencji pośrednika. Zawód jest zderegulowany od 2014 r., ale
    -- portale wciąż wystawiają to pole (Otodom: AgentLicense) i część biur je uzupełnia.
    license_number VARCHAR(50),

    created_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_agencies PRIMARY KEY (id)
);

ALTER TABLE users
    ADD COLUMN agency_id UUID;

-- Backfill: każde istniejące konto dostaje własną agencję o dotychczasowej nazwie.
-- Kont jest na tym etapie garść, a scalanie duplikatów nazw byłoby zgadywaniem —
-- dwa biura mogą nazywać się tak samo i nie są tym samym biurem.
INSERT INTO agencies (id, name, contact_email, created_at)
SELECT gen_random_uuid(), u.agency_name, u.email, u.created_at
FROM users u;

UPDATE users u
SET agency_id = a.id
FROM agencies a
WHERE a.contact_email = u.email;

ALTER TABLE users
    ALTER COLUMN agency_id SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_agency FOREIGN KEY (agency_id) REFERENCES agencies (id);

CREATE INDEX ix_users_agency ON users (agency_id);

-- users.agency_name zostaje celowo. Usunięcie go to osobna zmiana — najpierw kod
-- musi przestać go czytać, dopiero potem znika kolumna. Dwa kroki, nie jeden.
