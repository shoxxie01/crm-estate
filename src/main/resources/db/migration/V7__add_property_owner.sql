-- Powiązanie oferty z jej właścicielem-zleceniodawcą.
--
-- To ta relacja — nie kolumna na kliencie — niesie rozróżnienie
-- sprzedający/wynajmujący: wynika ono z transaction_type ofert danego
-- właściciela. Kolumna jest opcjonalna, bo oferta może powstać jako szkic,
-- zanim skojarzy się ją z klientem.

ALTER TABLE properties
    ADD COLUMN owner_client_id UUID;

ALTER TABLE properties
    ADD CONSTRAINT fk_properties_owner
        FOREIGN KEY (owner_client_id) REFERENCES clients (id);

CREATE INDEX ix_properties_owner ON properties (owner_client_id);
