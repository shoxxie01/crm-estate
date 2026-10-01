package pl.delta.crm.error;

/**
 * Klient nie istnieje albo należy do innego biura. Jak przy ofertach, celowo
 * nie rozróżniamy tych przypadków, żeby po kodzie odpowiedzi nie dało się
 * sprawdzać, czy dany identyfikator istnieje u konkurencji.
 */
public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException() {
        super("Nie znaleziono klienta.");
    }
}
