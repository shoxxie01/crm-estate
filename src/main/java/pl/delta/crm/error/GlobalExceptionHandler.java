package pl.delta.crm.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wszystkie błędy wychodzą jako ProblemDetail (RFC 7807). Front czyta `detail`
 * na komunikat ogólny, a opcjonalną mapę `errors` podpina pod konkretne pola.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Popraw zaznaczone pola.");
        problem.setTitle("Błąd walidacji");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ProblemDetail handleEmailTaken(EmailAlreadyUsedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Konflikt");
        problem.setProperty("errors", Map.of("email", "Ten adres e-mail jest już zajęty."));
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException exception) {
        // Ten sam komunikat dla złego hasła i nieistniejącego konta.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Nieprawidłowy e-mail lub hasło.");
        problem.setTitle("Brak autoryzacji");
        return problem;
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabled(DisabledException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Konto zostało zablokowane. Skontaktuj się z administratorem biura.");
        problem.setTitle("Konto nieaktywne");
        return problem;
    }
}
