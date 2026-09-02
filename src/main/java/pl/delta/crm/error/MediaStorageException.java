package pl.delta.crm.error;

/**
 * Storage plików nie odpowiedział albo odmówił zapisu. To awaria infrastruktury,
 * nie błąd danych użytkownika — stąd osobny typ i status 503 zamiast 400.
 */
public class MediaStorageException extends RuntimeException {

    public MediaStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
