package pl.delta.crm.error;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("Konto z tym adresem już istnieje.");
    }
}
