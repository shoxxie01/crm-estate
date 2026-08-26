package pl.delta.crm.property.dictionary;

/** Rodzaj materiału przypiętego do oferty. */
public enum MediaType implements Dictionary {

    PHOTO("Zdjęcie"),
    FLOOR_PLAN("Rzut"),
    DOCUMENT("Dokument");

    private final String label;

    MediaType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
