package pl.delta.crm.error;

/**
 * Transakcja nie istnieje albo należy do innego biura. Bez rozróżnienia,
 * z tego samego powodu co przy terminach i ofertach.
 */
public class DealNotFoundException extends RuntimeException {

    public DealNotFoundException() {
        super("Nie znaleziono transakcji.");
    }
}
