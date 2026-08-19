-- Pola specyficzne dla typów obiektu, które dotąd nie miały gdzie trafić:
-- parametry hali/magazynu oraz cechy garażu i wynajmowanego pokoju. Wszystkie
-- są opcjonalne (NULL), bo dotyczą tylko wybranych rodzajów ogłoszeń.

-- Hala / magazyn.
ALTER TABLE properties
    ADD COLUMN power_connection_kw  NUMERIC(8, 2),   -- moc przyłącza [kW]
    ADD COLUMN floor_load_t_per_m2  NUMERIC(6, 2),   -- nośność posadzki [t/m²]
    ADD COLUMN loading_docks_count  SMALLINT;        -- liczba bram / doków

-- Garaż / miejsce postojowe.
ALTER TABLE properties
    ADD COLUMN garage_type VARCHAR(30);

-- Pokój (wynajem).
ALTER TABLE properties
    ADD COLUMN occupants     SMALLINT,
    ADD COLUMN room_bathroom VARCHAR(20);
