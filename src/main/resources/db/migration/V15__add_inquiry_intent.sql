-- Formularz zgłoszeniowy obsługuje dwie intencje: kupno i sprzedaż.
--
-- Kupujący opisuje kryteria (jak dotąd, kolumna `criteria`), sprzedający —
-- nieruchomość, którą chce powierzyć biuru (`offer`). Oba jako JSON w kształcie
-- formularza: zgłoszenie jest poczekalnią, nikt nie filtruje po tych polach.
-- Zgłoszenia sprzedaży nie zamieniamy od razu na ofertę — ogłoszenie wymaga
-- danych, których właściciel w formularzu nie poda (adres, opis, zdjęcia),
-- więc agent zakłada ofertę po rozmowie.

ALTER TABLE client_inquiries ADD COLUMN intent VARCHAR(10) NOT NULL DEFAULT 'BUY';
ALTER TABLE client_inquiries ALTER COLUMN intent DROP DEFAULT;
ALTER TABLE client_inquiries ADD COLUMN offer TEXT;
ALTER TABLE client_inquiries ALTER COLUMN criteria DROP NOT NULL;

ALTER TABLE client_inquiries
    ADD CONSTRAINT ck_client_inquiries_intent CHECK (intent IN ('BUY', 'SELL'));

-- Każda intencja ma swoje dane — i tylko swoje.
ALTER TABLE client_inquiries
    ADD CONSTRAINT ck_client_inquiries_intent_payload CHECK (
        (intent = 'BUY' AND criteria IS NOT NULL AND offer IS NULL)
        OR (intent = 'SELL' AND offer IS NOT NULL AND criteria IS NULL)
    );
