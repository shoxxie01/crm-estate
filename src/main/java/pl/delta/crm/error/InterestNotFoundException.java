package pl.delta.crm.error;

/** Zainteresowany nie istnieje albo należy do innej transakcji. */
public class InterestNotFoundException extends RuntimeException {

    public InterestNotFoundException() {
        super("Nie znaleziono zainteresowanego.");
    }
}
