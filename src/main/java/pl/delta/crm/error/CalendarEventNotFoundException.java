package pl.delta.crm.error;

/**
 * Termin nie istnieje albo należy do innego biura. Jak przy ofertach
 * i klientach nie rozróżniamy tych przypadków, żeby po kodzie odpowiedzi nie
 * dało się sprawdzać, co ma w kalendarzu konkurencja.
 */
public class CalendarEventNotFoundException extends RuntimeException {

    public CalendarEventNotFoundException() {
        super("Nie znaleziono terminu.");
    }
}
