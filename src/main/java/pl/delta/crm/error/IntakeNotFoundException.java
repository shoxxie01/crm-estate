package pl.delta.crm.error;

/** Nieznany klucz formularza. Link literówką albo wymieniony przez administratora biura. */
public class IntakeNotFoundException extends RuntimeException {

    public IntakeNotFoundException() {
        super("Ten formularz nie istnieje albo link przestał być aktualny. Poproś biuro o nowy.");
    }
}
