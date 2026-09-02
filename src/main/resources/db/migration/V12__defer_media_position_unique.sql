-- Kolejność zdjęć w galerii zmienia się przez przepisanie pozycji wielu wierszom
-- naraz. Przy natychmiastowym unikacie każda taka operacja wywala się w połowie:
-- przesunięcie zdjęcia z pozycji 3 na 0 wymaga chwilowego stanu, w którym dwa
-- wiersze mają tę samą pozycję, nawet jeśli stan końcowy jest poprawny.
--
-- Odroczenie do commitu przenosi sprawdzenie na moment, w którym przepisanie
-- jest już kompletne. Alternatywą byłoby przestawianie przez wartości tymczasowe
-- (np. ujemne), czyli podwójna liczba UPDATE-ów i logika istniejąca wyłącznie
-- po to, żeby obejść ograniczenie bazy.
--
-- UNIQUE INDEX nie da się odroczyć — musi to być CONSTRAINT.
DROP INDEX ux_property_media_position;

ALTER TABLE property_media
    ADD CONSTRAINT ux_property_media_position UNIQUE (property_id, position)
        DEFERRABLE INITIALLY DEFERRED;
