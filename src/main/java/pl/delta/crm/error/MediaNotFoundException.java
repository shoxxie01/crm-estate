package pl.delta.crm.error;

/**
 * Zdjęcie nie istnieje albo należy do oferty innego biura. Tak samo jak przy
 * ofertach, jedno i drugie wychodzi jako 404.
 */
public class MediaNotFoundException extends RuntimeException {

    public MediaNotFoundException() {
        super("Nie znaleziono zdjęcia.");
    }
}
