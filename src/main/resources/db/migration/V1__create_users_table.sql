CREATE TABLE users
(
    id            UUID         NOT NULL,
    email         VARCHAR(190) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    first_name    VARCHAR(80)  NOT NULL,
    last_name     VARCHAR(80)  NOT NULL,
    agency_name   VARCHAR(150) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ck_users_role CHECK (role IN ('AGENT', 'MANAGER', 'ADMIN'))
);

-- E-mail jest loginem. Aplikacja normalizuje go do małych liter przed zapisem
-- i przed wyszukaniem, więc zwykły unique wystarcza — bez indeksu na lower(email).
CREATE UNIQUE INDEX ux_users_email ON users (email);
