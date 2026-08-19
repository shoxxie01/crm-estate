package pl.delta.crm.property.dto;

import java.util.List;

/** Cechy pogrupowane kategorią — front rysuje z tego sekcje formularza. */
public record FeatureGroup(String category, String label, List<FeatureView> features) {

    /**
     * Jedna cecha wraz z typami obiektu, dla których ma sens. Front pokazuje
     * w sekcji „Cechy" tylko cechy pasujące do wybranego rodzaju nieruchomości.
     */
    public record FeatureView(String value, String label, List<String> types) {
    }
}
