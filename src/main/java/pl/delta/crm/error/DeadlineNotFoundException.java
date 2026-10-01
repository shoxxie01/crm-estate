package pl.delta.crm.error;

/** Termin umowny nie istnieje albo należy do innej transakcji. */
public class DeadlineNotFoundException extends RuntimeException {

    public DeadlineNotFoundException() {
        super("Nie znaleziono terminu umownego.");
    }
}
