package pl.delta.crm.property.dictionary;

/**
 * Stan publikacji oferty w konkretnym portalu.
 *
 * <p>Stan jest odrębny od {@link PropertyStatus}, bo publikacja jest
 * asynchroniczna: Otodom odbiera paczkę przez FTP i przetwarza ją mniej więcej
 * co godzinę, a wynik odsyła dopiero w raporcie. Między wysyłką a raportem
 * oferta jest w {@link #WYSLANA} — ani opublikowana, ani błędna.
 */
public enum PublicationStatus implements Dictionary {

    NIEOPUBLIKOWANA("Nieopublikowana"),
    DO_WYSLANIA("Do wysłania"),
    WYSLANA("Wysłana — czeka na potwierdzenie"),
    OPUBLIKOWANA("Opublikowana"),
    BLAD("Błąd"),
    WYCOFANA("Wycofana");

    private final String label;

    PublicationStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
