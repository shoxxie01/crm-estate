package pl.delta.crm.inquiry;

import pl.delta.crm.agency.Agency;

/**
 * Treść klauzul formularza zgłoszeniowego.
 *
 * <p>Jedno źródło dla formularza i dla zapisu: front pokazuje dokładnie tekst,
 * który serwer potem zapisuje przy zgłoszeniu jako dowód zgody. Gdyby front
 * trzymał własną kopię, zmiana po jednej stronie rozjechałaby to, co klient
 * widział, z tym, co mamy w bazie.
 *
 * <p>Przed produkcją treść powinien zatwierdzić prawnik biura — to wersja
 * robocza, zgodna z duchem RODO, ale nie porada prawna.
 */
public final class IntakeConsent {

    private IntakeConsent() {
        // klasa narzędziowa
    }

    public static String processing(Agency agency) {
        return "Wyrażam zgodę na przetwarzanie moich danych osobowych podanych w formularzu przez "
                + agency.getName()
                + " w celu obsługi zgłoszenia i kontaktu w sprawie kupna lub sprzedaży nieruchomości.";
    }

    public static String marketing(Agency agency) {
        return "Chcę otrzymywać od " + agency.getName()
                + " informacje o nowych ofertach nieruchomości telefonicznie i e-mailem.";
    }

    public static String privacyNote(Agency agency) {
        String contact = agency.getContactEmail() == null ? "" : " (" + agency.getContactEmail() + ")";
        return "Administratorem danych jest " + agency.getName() + contact
                + ". Zgodę możesz w każdej chwili wycofać, a także zażądać wglądu w swoje dane,"
                + " ich poprawienia lub usunięcia — wystarczy wiadomość do biura.";
    }
}
