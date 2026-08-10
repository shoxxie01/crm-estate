package pl.delta.crm.property.dictionary;

/** Rodzaj materiału przypiętego do oferty. */
public enum MediaType implements Dictionary {

    ZDJECIE("Zdjęcie"),
    RZUT("Rzut"),
    DOKUMENT("Dokument");

    private final String label;

    MediaType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
