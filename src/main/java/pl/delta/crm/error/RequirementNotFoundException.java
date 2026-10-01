package pl.delta.crm.error;

/**
 * Poszukiwanie nie istnieje, należy do innego klienta albo do innego biura.
 * Jak przy klientach, tych przypadków celowo nie rozróżniamy.
 */
public class RequirementNotFoundException extends RuntimeException {

    public RequirementNotFoundException() {
        super("Nie znaleziono poszukiwania.");
    }
}
