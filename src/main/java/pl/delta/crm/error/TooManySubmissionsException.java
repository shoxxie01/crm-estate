package pl.delta.crm.error;

/** Przekroczony limit zgłoszeń z jednego adresu. */
public class TooManySubmissionsException extends RuntimeException {

    public TooManySubmissionsException() {
        super("Wysłano zbyt wiele zgłoszeń w krótkim czasie. Spróbuj ponownie za kilka minut.");
    }
}
