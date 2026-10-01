package pl.delta.crm.property.dictionary;

/**
 * Kategoria cechy. Portale nie mają jednej listy udogodnień. Rozbijają je na
 * osobne maski w XML-u (Otodom: ExtrasMask, SecurityMask, MediaMask,
 * EquipmentMask, VicinityMask, FenceMask). U nas cechy leżą w jednym zbiorze,
 * a kategoria pozwala eksportowi rozłożyć je z powrotem na właściwe tagi.
 */
public enum FeatureCategory implements Dictionary {

    ADDITIONAL("Informacje dodatkowe"),
    SECURITY("Zabezpieczenia"),
    UTILITIES("Media"),
    EQUIPMENT("Wyposażenie"),
    NEIGHBOURHOOD("Okolica"),
    FENCING("Ogrodzenie");

    private final String label;

    FeatureCategory(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
