package pl.delta.crm.error;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    @ExceptionHandler(BusinessValidationException.class)
    public ProblemDetail handleBusinessValidation(BusinessValidationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Błąd walidacji");
        problem.setProperty("errors", exception.getErrors());
        return problem;
    }

    @ExceptionHandler(PropertyNotFoundException.class)
    public ProblemDetail handlePropertyNotFound(PropertyNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Nie znaleziono");
        return problem;
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ProblemDetail handleClientNotFound(ClientNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Nie znaleziono");
        return problem;
    }

    @ExceptionHandler(MediaNotFoundException.class)
    public ProblemDetail handleMediaNotFound(MediaNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Nie znaleziono");
        return problem;
    }

    /**
     * Storage nie odpowiedział. Osobno od błędów walidacji, bo użytkownik nie ma
     * tu czego poprawić — ma spróbować ponownie. Treść wyjątku nie idzie na
     * zewnątrz, poszłyby w niej adresy i nazwy bucketów.
     */
    @ExceptionHandler(MediaStorageException.class)
    public ProblemDetail handleStorageFailure(MediaStorageException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Magazyn plików jest chwilowo niedostępny. Spróbuj wgrać zdjęcie ponownie za chwilę.");
        problem.setTitle("Storage niedostępny");
        return problem;
    }

    /**
     * Plik większy niż limit multiparta. Bez tej obsługi wracał surowy błąd
     * kontenera, a formularz nie miał czego pokazać przy polu.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE, "Plik jest za duży. Maksymalny rozmiar zdjęcia to 15 MB.");
        problem.setTitle("Za duży plik");
        problem.setProperty("errors", Map.of("files", "Plik jest za duży. Maksymalny rozmiar zdjęcia to 15 MB."));
        return problem;
    }

    @ExceptionHandler(CalendarEventNotFoundException.class)
    public ProblemDetail handleCalendarEventNotFound(CalendarEventNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Nie znaleziono");
        return problem;
    }

    /**
     * Ciało żądania, którego nie da się odczytać — uszkodzony JSON albo wartość
     * spoza słownika w polu enumowym. Bez tej obsługi błąd trafiał do domyślnego
     * `/error` i wracał w innym kształcie niż reszta API, więc front nie miał
     * czego pokazać poza komunikatem zastępczym.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Nie udało się odczytać danych formularza. Sprawdź, czy wszystkie pola mają poprawne wartości.");
        problem.setTitle("Błędne dane");
        return problem;
    }

    /**
     * Naruszenie więzów bazy, którego nie wychwyciła wcześniejsza walidacja.
     * Nie pokazujemy treści błędu SQL — poszłyby w niej nazwy kolumn i tabel.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Nie udało się zapisać danych — sprawdź poprawność pól.");
        problem.setTitle("Konflikt danych");
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
