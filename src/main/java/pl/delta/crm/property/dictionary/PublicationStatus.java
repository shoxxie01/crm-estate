package pl.delta.crm.property.dictionary;

/**
 * Stan publikacji oferty w konkretnym portalu.
 *
 * <p>Stan jest odrębny od {@link PropertyStatus}, bo publikacja jest
 * asynchroniczna: Otodom odbiera paczkę przez FTP i przetwarza ją mniej więcej
 * co godzinę, a wynik odsyła dopiero w raporcie. Między wysyłką a raportem
 * oferta jest w {@link #SENT}. Ani opublikowana, ani błędna.
 */
public enum PublicationStatus implements Dictionary {

    NOT_PUBLISHED("Nieopublikowana"),
    PENDING_SEND("Do wysłania"),
    SENT("Wysłana. Czeka na potwierdzenie"),
    PUBLISHED("Opublikowana"),
    ERROR("Błąd"),
    WITHDRAWN("Wycofana");

    private final String label;

    PublicationStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
