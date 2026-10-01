package pl.delta.crm.error;

/** Zgłoszenie nie istnieje albo należy do innego biura. Tych przypadków nie rozróżniamy. */
public class InquiryNotFoundException extends RuntimeException {

    public InquiryNotFoundException() {
        super("Nie znaleziono zgłoszenia.");
    }
}
