package pl.delta.crm.error;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Błąd walidacji zależności między polami — czegoś, czego Bean Validation nie
 * widzi, bo warunek nie dotyczy jednego pola, tylko relacji między kilkoma
 * (np. „liczba pokoi jest wymagana, ale tylko dla mieszkania i domu").
 *
 * <p>Wychodzi na zewnątrz w tym samym kształcie co błędy adnotacji, więc front
 * podpina komunikaty pod pola tym samym kodem.
 */
public class BusinessValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public BusinessValidationException(Map<String, String> errors) {
        super("Popraw zaznaczone pola.");
        this.errors = new LinkedHashMap<>(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
