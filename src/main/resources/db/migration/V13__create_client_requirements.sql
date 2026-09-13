-- Poszukiwania klientów — strona popytu.
--
-- Do tej pory `clients` było wyłącznie stroną podaży (V6). Kupujący i najemcy
-- trafiają do tej samej tabeli, bo to często te same osoby (właściciel sprzedaje
-- kawalerkę i szuka większego mieszkania). Rola nadal NIE jest kolumną klienta:
-- sprzedający / wynajmujący wynika z jego ofert, kupujący / najemca — z jego
-- aktywnych poszukiwań.
--
-- Jeden klient może szukać kilku rzeczy naraz (mieszkanie do kupienia i garaż
-- do najmu), dlatego poszukiwanie jest osobnym wierszem, a nie kolumnami klienta.
-- Kryteria są zwykłymi kolumnami, a nie JSON-em: dopasowanie „kto szuka czegoś
-- takiego jak ta oferta" musi dać się napisać w SQL i zaindeksować.

CREATE TABLE client_requirements
(
    id                UUID          NOT NULL,
    agency_id         UUID          NOT NULL,
    client_id         UUID          NOT NULL,
    created_by        UUID          NOT NULL,

    status            VARCHAR(20)   NOT NULL,
    transaction_type  VARCHAR(20)   NOT NULL,
    -- NULL = rynek obojętny.
    market_type       VARCHAR(20),

    -- Każda granica z osobna opcjonalna („do 600 tys.", „od 50 m²").
    -- Budżet w PLN: przy kupnie cena, przy najmie miesięczny czynsz.
    price_min         NUMERIC(14, 2),
    price_max         NUMERIC(14, 2),
    area_min          NUMERIC(10, 2),
    area_max          NUMERIC(10, 2),
    rooms_min         SMALLINT,
    rooms_max         SMALLINT,
    -- Jak properties.floor_no: -1 suterena, 0 parter.
    floor_min         SMALLINT,
    floor_max         SMALLINT,
    exclude_top_floor BOOLEAN       NOT NULL DEFAULT FALSE,

    -- Sposób finansowania ma sens tylko przy kupnie — mówi agentowi, na ile
    -- klient jest gotowy do transakcji.
    financing         VARCHAR(20),
    move_in_date      DATE,
    notes             TEXT,

    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_client_requirements PRIMARY KEY (id),
    CONSTRAINT fk_client_requirements_agency FOREIGN KEY (agency_id) REFERENCES agencies (id),
    -- Poszukiwanie nie ma sensu bez osoby, która szuka — znika razem z klientem.
    CONSTRAINT fk_client_requirements_client
        FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE,
    CONSTRAINT fk_client_requirements_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_client_requirements_status
        CHECK (status IN ('ACTIVE', 'PAUSED', 'FULFILLED', 'CLOSED')),
    CONSTRAINT ck_client_requirements_transaction CHECK (transaction_type IN ('SALE', 'RENT')),
    CONSTRAINT ck_client_requirements_financing CHECK (financing IS NULL OR transaction_type = 'SALE'),
    CONSTRAINT ck_client_requirements_price CHECK (price_min IS NULL OR price_max IS NULL OR price_min <= price_max),
    CONSTRAINT ck_client_requirements_area CHECK (area_min IS NULL OR area_max IS NULL OR area_min <= area_max),
    CONSTRAINT ck_client_requirements_rooms CHECK (rooms_min IS NULL OR rooms_max IS NULL OR rooms_min <= rooms_max),
    CONSTRAINT ck_client_requirements_floor CHECK (floor_min IS NULL OR floor_max IS NULL OR floor_min <= floor_max)
);

-- Karta klienta.
CREATE INDEX ix_client_requirements_client ON client_requirements (client_id);
-- Przyszłe dopasowanie do ofert: zawsze w obrębie biura, tylko aktywne, po typie transakcji.
CREATE INDEX ix_client_requirements_matching ON client_requirements (agency_id, status, transaction_type);

-- Rodzaje nieruchomości — zwykle jeden, ale „mieszkanie albo dom" to częsta odpowiedź.
CREATE TABLE client_requirement_property_types
(
    requirement_id UUID        NOT NULL,
    property_type  VARCHAR(30) NOT NULL,

    CONSTRAINT pk_client_requirement_property_types PRIMARY KEY (requirement_id, property_type),
    CONSTRAINT fk_client_requirement_property_types_requirement
        FOREIGN KEY (requirement_id) REFERENCES client_requirements (id) ON DELETE CASCADE
);

-- Lokalizacje w kolejności podanej przez klienta — pierwsza jest najważniejsza.
-- Miasto i dzielnica osobno, tak jak w adresie oferty, żeby dało się je porównać.
CREATE TABLE client_requirement_locations
(
    requirement_id UUID        NOT NULL,
    position       INTEGER     NOT NULL,
    city           VARCHAR(80) NOT NULL,
    district       VARCHAR(64),

    CONSTRAINT pk_client_requirement_locations PRIMARY KEY (requirement_id, position),
    CONSTRAINT fk_client_requirement_locations_requirement
        FOREIGN KEY (requirement_id) REFERENCES client_requirements (id) ON DELETE CASCADE
);

-- Cechy ze słownika ofert. Jedna tabela z flagą zamiast dwóch: klucz główny
-- sam pilnuje, żeby cecha nie była naraz „konieczna" i „mile widziana".
CREATE TABLE client_requirement_features
(
    requirement_id UUID        NOT NULL,
    feature        VARCHAR(40) NOT NULL,
    required       BOOLEAN     NOT NULL,

    CONSTRAINT pk_client_requirement_features PRIMARY KEY (requirement_id, feature),
    CONSTRAINT fk_client_requirement_features_requirement
        FOREIGN KEY (requirement_id) REFERENCES client_requirements (id) ON DELETE CASCADE
);
