package pl.delta.crm.storage;

/**
 * Warstwa plików widziana przez resztę aplikacji. Celowo wąska i bez śladu S3
 * w sygnaturach. Moduł ofert ma wiedzieć, że plik da się zapisać, skasować
 * i pokazać, a nie czym to jest zaimplementowane.
 *
 * <p>Klucz obiektu jest jedynym identyfikatorem pliku i trafia do kolumny
 * {@code property_media.storage_key}. Nie ma tu metody czytającej zawartość:
 * przeglądarka pobiera zdjęcia bezpośrednio ze storage'u po podpisany link,
 * więc bajty nie przechodzą przez aplikację. Moduł eksportu, gdy powstanie,
 * dostanie osobną metodę strumieniującą do paczki ZIP.
 */
public interface MediaStorage {

    /** Zapisuje (lub nadpisuje) obiekt pod danym kluczem. */
    void put(String key, byte[] content, String contentType);

    /**
     * Kasuje obiekt. Brak obiektu nie jest błędem. Wiersz w bazie bez pliku psuje
     * galerię, plik bez wiersza jest tylko zajętym miejscem, więc kasowanie ma
     * prawo być bezszelestne.
     */
    void delete(String key);

    /**
     * Czasowy, podpisany link do odczytu. Działa bez tokenu aplikacji, dlatego
     * ma krótki TTL. To on, a nie autoryzacja, ogranicza czas życia dostępu.
     */
    String url(String key);
}
