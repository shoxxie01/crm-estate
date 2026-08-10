-- Nieruchomości.
--
-- Zestaw pól jest podyktowany tym, czego wymagają portale ogłoszeniowe przy
-- imporcie ofert (Otodom Import, Morizon, nieruchomosci-online NOE) — dołożenie
-- kolumny do tabeli z tysiącami ofert i wypełnienie jej wstecz jest znacznie
-- droższe niż zaprojektowanie jej teraz.
--
-- ZASADA NADRZĘDNA: w bazie nie ma ani jednego kodu portalowego.
-- Portale opisują ten sam atrybut różnymi słownikami, a Otodom dodatkowo używa
-- różnych słowników dla różnych typów obiektu (BuildingType dla mieszkania to
-- inny słownik niż BuildingType dla domu i jeszcze inny dla lokalu). Do tego
-- dokumentacja Otodom wprost zaleca „częste aktualizacje słowników". Gdybyśmy
-- zapisali liczbę 2, po zmianie słownika u nich cała baza znaczyłaby co innego.
-- Trzymamy więc własne nazwy, a tłumaczenie na kody portalu robi moduł eksportu.

CREATE TABLE properties
(
    id                      UUID         NOT NULL,
    agency_id               UUID         NOT NULL,

    -- Numer oferty w biurze. Otodom przekazuje go jako <ID> i wymaga
    -- unikalności oraz max. 32 znaków — to po nim portal rozpoznaje, czy
    -- ogłoszenie aktualizować, czy założyć nowe.
    reference_number        VARCHAR(32)  NOT NULL,

    -- Kto prowadzi ofertę. Z tego agenta biorą się dane kontaktowe w ogłoszeniu.
    agent_id                UUID         NOT NULL,
    created_by              UUID         NOT NULL,

    ------------------------------------------------------------------ klasyfikacja
    -- Rodzaj obiektu. Determinuje, który zestaw pól szczegółowych ma sens
    -- i który tag Details trafi do XML-a (Otodom: ObjectName).
    property_type           VARCHAR(30)  NOT NULL,
    -- Sprzedaż / wynajem (Otodom: OfferType).
    transaction_type        VARCHAR(20)  NOT NULL,
    -- Rynek pierwotny / wtórny (Otodom: MarketType). Pole WYMAGANE przez portal.
    market_type             VARCHAR(20)  NOT NULL,
    -- Status wewnętrzny CRM-u. Na portale idą wyłącznie oferty AKTYWNA.
    status                  VARCHAR(20)  NOT NULL,

    ------------------------------------------------------------------ treść ogłoszenia
    -- Otodom obcina tytuł do 50 znaków — trzymamy ten limit u siebie, żeby
    -- agent zobaczył problem przy wprowadzaniu, a nie po eksporcie.
    title                   VARCHAR(50)  NOT NULL,
    -- Opis bez limitu długości po stronie Otodom, ale portal wycina tagi HTML.
    description             TEXT         NOT NULL,

    ------------------------------------------------------------------ finanse
    price                   NUMERIC(14, 2) NOT NULL,
    price_currency          VARCHAR(3)   NOT NULL,
    price_negotiable        BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Czynsz administracyjny. Osobno od ceny, bo portal pokazuje go osobno.
    rent                    NUMERIC(12, 2),
    rent_currency           VARCHAR(3),
    -- Czy cena najmu zawiera już czynsz (Otodom: PriceIncludeRent).
    price_includes_rent     BOOLEAN      NOT NULL DEFAULT FALSE,
    deposit                 NUMERIC(12, 2),
    deposit_currency        VARCHAR(3),
    -- Prowizja biura. Dane wewnętrzne, nie idą do ogłoszenia.
    commission_percent      NUMERIC(5, 2),

    ------------------------------------------------------------------ powierzchnie i układ
    -- Powierzchnia całkowita — pole WYMAGANE przez Otodom dla każdego typu
    -- poza pokojem (tag Area).
    total_area              NUMERIC(10, 2) NOT NULL,
    usable_area             NUMERIC(10, 2),
    -- Powierzchnia działki (Otodom: TerrainArea) — dla domu i lokalu/obiektu.
    plot_area               NUMERIC(12, 2),
    -- WYMAGANE przez Otodom dla mieszkania i domu (RoomsNum).
    rooms_count             SMALLINT,
    bathrooms_count         SMALLINT,
    -- Piętro. Umowa: -1 = suterena, 0 = parter, dalej numer piętra.
    -- Otodom ma na to słownik z przesunięciem (0=suterena, 1=parter, 2=„1"),
    -- ale przesunięcie to szczegół eksportu, nie modelu.
    floor_no                SMALLINT,
    building_floors_count   SMALLINT,
    -- Wysokość pomieszczeń w metrach — istotna dla hal i lokali użytkowych.
    ceiling_height          NUMERIC(5, 2),

    ------------------------------------------------------------------ budynek
    build_year              SMALLINT,
    building_type           VARCHAR(40),
    building_material       VARCHAR(30),
    construction_status     VARCHAR(30),
    windows_type            VARCHAR(20),
    roof_type               VARCHAR(20),
    roofing                 VARCHAR(20),
    garret_type             VARCHAR(20),
    -- Forma własności (Otodom: BuildingOwnership). Dla polskiego rynku
    -- kluczowe rozróżnienie: pełna własność vs spółdzielcze własnościowe.
    ownership_form          VARCHAR(40),
    -- Położenie: miasto / pod miastem / wieś (Otodom HouseDetails: Location).
    surroundings            VARCHAR(20),
    furnished               BOOLEAN,
    -- Od kiedy wolne (Otodom: FreeFrom).
    available_from          DATE,

    ------------------------------------------------------------------ lokalizacja
    -- Otodom wymaga kompletu Country + Province + District + City.
    -- Province i District mają u nich własne słowniki, ale przyjmują też
    -- nazwę tekstową — dlatego trzymamy nazwy administracyjne, nie kody portalu.
    country_code            VARCHAR(2)   NOT NULL DEFAULT 'PL',
    voivodeship             VARCHAR(30)  NOT NULL,
    county                  VARCHAR(80)  NOT NULL,
    commune                 VARCHAR(80),
    city                    VARCHAR(80)  NOT NULL,
    district                VARCHAR(64),
    street                  VARCHAR(64),
    building_number         VARCHAR(20),
    -- Numer lokalu nigdy nie trafia do ogłoszenia. Jest do kontaktu z właścicielem.
    apartment_number        VARCHAR(20),
    postal_code             VARCHAR(6),
    latitude                NUMERIC(9, 6),
    longitude               NUMERIC(9, 6),
    -- Czy publikować dokładny adres. Właściciele często nie chcą numeru budynku
    -- w ogłoszeniu — portal dostaje wtedy samą ulicę albo dzielnicę.
    hide_exact_address      BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Kody TERYT. Nie wymagane przez portale, ale jednoznacznie identyfikują
    -- miejscowość i ulicę, więc automat mapujący na słowniki portalu
    -- ma się czego chwycić zamiast dopasowywać nazwy tekstem.
    teryt_simc              VARCHAR(7),
    teryt_ulic              VARCHAR(5),

    ------------------------------------------------------------------ charakterystyka energetyczna
    -- Od 28.04.2023 świadectwo jest w Polsce obowiązkowe przy sprzedaży i najmie,
    -- a wskaźnik EP musi być podany w ogłoszeniu (ustawa o charakterystyce
    -- energetycznej budynków). Specyfikacja Otodom Import jest starsza (2017) i
    -- tych pól nie ma — ale obowiązek prawny istnieje niezależnie od formatu XML,
    -- a nowsze integracje portali już je przyjmują.
    energy_ep               NUMERIC(7, 2),   -- kWh/(m²·rok), energia pierwotna
    energy_ek               NUMERIC(7, 2),   -- kWh/(m²·rok), energia końcowa
    energy_class            VARCHAR(3),
    energy_cert_number      VARCHAR(60),
    energy_cert_issued_at   DATE,
    energy_cert_valid_until DATE,
    -- Zwolnienie z obowiązku (np. zabytek, budynek do 50 m², sakralny).
    energy_cert_exempt      BOOLEAN      NOT NULL DEFAULT FALSE,
    energy_cert_exempt_note VARCHAR(200),

    ------------------------------------------------------------------ działka
    plot_type               VARCHAR(30),
    -- Wymiary jako tekst „szer x dł" — Otodom przyjmuje dokładnie taki string.
    plot_dimensions         VARCHAR(32),
    road_access             VARCHAR(20),
    plot_fenced             BOOLEAN,
    -- Przeznaczenie w miejscowym planie zagospodarowania.
    zoning_plan             VARCHAR(200),

    ------------------------------------------------------------------ lokal / hala
    -- Konstrukcja hali (stalowa, murowana, wiata...).
    hall_structure          VARCHAR(20),
    hall_flooring           VARCHAR(20),
    parking_type            VARCHAR(20),
    has_office_space        BOOLEAN,
    has_social_facilities   BOOLEAN,
    has_loading_ramp        BOOLEAN,

    ------------------------------------------------------------------ media ogłoszenia
    video_url               VARCHAR(500),
    -- Wirtualny spacer / panorama (Otodom: Panorama).
    panorama_url            VARCHAR(500),

    ------------------------------------------------------------------ dane wewnętrzne
    -- Nie idą do żadnego ogłoszenia.
    private_notes           TEXT,
    keys_info               VARCHAR(200),
    -- Blokada eksportu dla ofert, które mają zostać wyłącznie w CRM-ie.
    exportable              BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at              TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_properties PRIMARY KEY (id),
    CONSTRAINT fk_properties_agency FOREIGN KEY (agency_id) REFERENCES agencies (id),
    CONSTRAINT fk_properties_agent FOREIGN KEY (agent_id) REFERENCES users (id),
    CONSTRAINT fk_properties_created_by FOREIGN KEY (created_by) REFERENCES users (id),

    CONSTRAINT ck_properties_price CHECK (price > 0),
    CONSTRAINT ck_properties_total_area CHECK (total_area > 0),
    CONSTRAINT ck_properties_floors CHECK (
        floor_no IS NULL OR building_floors_count IS NULL OR floor_no <= building_floors_count),
    CONSTRAINT ck_properties_geo CHECK (
        (latitude IS NULL AND longitude IS NULL)
            OR (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180))
);

-- Numer oferty jest unikalny w obrębie biura, nie globalnie — dwa biura mogą
-- niezależnie prowadzić ofertę „12/2026".
CREATE UNIQUE INDEX ux_properties_reference ON properties (agency_id, reference_number);

-- Lista ofert biura jest zawsze filtrowana po agencji i sortowana po dacie.
CREATE INDEX ix_properties_agency_created ON properties (agency_id, created_at DESC);
CREATE INDEX ix_properties_agency_status ON properties (agency_id, status);
CREATE INDEX ix_properties_agent ON properties (agent_id);

-- Cechy wielowartościowe. Portale rozbijają je na osobne maski (ExtrasMask,
-- SecurityMask, MediaMask, EquipmentMask...), ale u nas to jeden zbiór —
-- kategoria siedzi w samym enumie, więc eksport potrafi go z powrotem rozbić.
CREATE TABLE property_features
(
    property_id UUID        NOT NULL,
    feature     VARCHAR(40) NOT NULL,

    CONSTRAINT pk_property_features PRIMARY KEY (property_id, feature),
    CONSTRAINT fk_property_features_property
        FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
);

-- Ogrzewanie jest wielowartościowe, bo dom potrafi mieć gazowe i kominkowe naraz
-- (Otodom HouseDetails: HeatingMask). Dla mieszkania portal przyjmuje jedną
-- wartość — eksport wybiera wtedy pierwszą.
CREATE TABLE property_heating
(
    property_id  UUID        NOT NULL,
    heating_type VARCHAR(30) NOT NULL,

    CONSTRAINT pk_property_heating PRIMARY KEY (property_id, heating_type),
    CONSTRAINT fk_property_heating_property
        FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
);

-- Przeznaczenie lokalu/hali (Otodom: PropertyUseMask, UseMask) — też wiele naraz.
CREATE TABLE property_commercial_uses
(
    property_id     UUID        NOT NULL,
    commercial_use  VARCHAR(30) NOT NULL,

    CONSTRAINT pk_property_commercial_uses PRIMARY KEY (property_id, commercial_use),
    CONSTRAINT fk_property_commercial_uses_property
        FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
);

-- Zdjęcia i pozostałe materiały.
CREATE TABLE property_media
(
    id           UUID         NOT NULL,
    property_id  UUID         NOT NULL,
    media_type   VARCHAR(20)  NOT NULL,
    -- Ścieżka w naszym storage. Paczka dla Otodom pakuje pliki płasko do ZIP-a,
    -- więc nazwa pliku musi dać się wyprowadzić z tej wartości.
    storage_key  VARCHAR(300) NOT NULL,
    file_name    VARCHAR(120) NOT NULL,
    content_type VARCHAR(60)  NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    width_px     INTEGER,
    height_px    INTEGER,
    -- Kolejność w galerii. Otodom sortuje po tagu Position, pierwsze zdjęcie
    -- jest zdjęciem głównym oferty.
    position     SMALLINT     NOT NULL,
    caption      VARCHAR(150),
    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_property_media PRIMARY KEY (id),
    CONSTRAINT fk_property_media_property
        FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE,
    CONSTRAINT ck_property_media_position CHECK (position >= 0)
);

CREATE UNIQUE INDEX ux_property_media_position ON property_media (property_id, position);

-- Stan publikacji per portal. Jedna oferta żyje w kilku serwisach naraz i w
-- każdym ma inny identyfikator zewnętrzny oraz własny stan wysyłki.
CREATE TABLE property_portal_publications
(
    id                 UUID        NOT NULL,
    property_id        UUID        NOT NULL,
    portal             VARCHAR(30) NOT NULL,
    status             VARCHAR(20) NOT NULL,
    -- Identyfikator ogłoszenia nadany przez portal, o ile go zwraca.
    external_id        VARCHAR(64),
    external_url       VARCHAR(500),
    last_exported_at   TIMESTAMP(6) WITH TIME ZONE,
    -- Treść błędu z raportu importu — portale odsyłają go asynchronicznie
    -- (Otodom przetwarza paczki z FTP mniej więcej co godzinę).
    last_error         VARCHAR(500),

    CONSTRAINT pk_property_portal_publications PRIMARY KEY (id),
    CONSTRAINT fk_property_publications_property
        FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_property_publications ON property_portal_publications (property_id, portal);
