-- Powiat przestaje być wymagany przy zapisie oferty.
--
-- Otodom nadal wymaga go do ustalenia lokalizacji (Country + Province +
-- District + City), ale to jest warunek *publikacji*, nie warunek istnienia
-- oferty w CRM-ie. Agent często zakłada ofertę z telefonu od właściciela,
-- mając miasto i ulicę, a powiat uzupełnia później. Blokowanie zapisu na tym
-- polu wypychałoby takie oferty poza system, do notatnika.
--
-- Kompletność pod kątem eksportu pilnuje teraz Property.readyForExport().
ALTER TABLE properties
    ALTER COLUMN county DROP NOT NULL;

-- reference_number zostaje NOT NULL i unikalny w obrębie biura — od tej zmiany
-- nadaje go aplikacja, a nie użytkownik. Portal rozpoznaje po nim ogłoszenie
-- przy kolejnych wysyłkach; bez stabilnego numeru każdy eksport zakładałby
-- nowe ogłoszenie zamiast aktualizować istniejące.
