package pl.delta.crm.error;

/**
 * Geokoder nie odpowiedział albo odmówił obsługi. Dla oferty to nie jest błąd
 * krytyczny — adres można wpisać ręcznie, a pinezkę postawić palcem — więc
 * front pokazuje to jako komunikat przy mapie, nie jako nieudany zapis.
 */
public class GeoUnavailableException extends RuntimeException {

    public GeoUnavailableException(String message) {
        super(message);
    }

    public GeoUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
