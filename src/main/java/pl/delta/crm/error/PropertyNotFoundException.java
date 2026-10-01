package pl.delta.crm.error;

/**
 * Oferta nie istnieje albo należy do innego biura. Celowo nie rozróżniamy tych
 * dwóch przypadków, żeby po samym kodzie odpowiedzi nie dało się sprawdzać,
 * czy dany identyfikator istnieje u konkurencji.
 */
public class PropertyNotFoundException extends RuntimeException {

    public PropertyNotFoundException() {
        super("Nie znaleziono oferty.");
    }
}
