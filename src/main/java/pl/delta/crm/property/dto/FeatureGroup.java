package pl.delta.crm.property.dto;

import java.util.List;

/** Cechy pogrupowane kategorią — front rysuje z tego sekcje formularza. */
public record FeatureGroup(String category, String label, List<DictionaryEntry> features) {
}
