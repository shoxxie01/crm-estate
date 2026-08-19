-- Cena za metr kwadratowy zapisywana wprost (dotąd liczona w locie z ceny i
-- powierzchni). Trzymamy ją, bo wpisuje się ją ręcznie ALBO wylicza automatycznie
-- w formularzu, a lista ofert sortuje po tej wartości bez ponownego dzielenia.
ALTER TABLE properties
    ADD COLUMN price_per_m2 NUMERIC(14, 2);

-- Uzupełnienie istniejących ofert: cena / powierzchnia całkowita, zaokrąglone
-- do dwóch miejsc (total_area ma constraint > 0, więc dzielenie jest bezpieczne).
UPDATE properties
SET price_per_m2 = ROUND(price / total_area, 2);
