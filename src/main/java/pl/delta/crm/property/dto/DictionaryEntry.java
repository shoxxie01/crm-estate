package pl.delta.crm.property.dto;

import pl.delta.crm.property.dictionary.Dictionary;

import java.util.Arrays;
import java.util.List;

/**
 * Jedna pozycja słownika: stała wartość techniczna + polska etykieta.
 * Front buduje z tego listy wyboru, zamiast powielać enumy w TypeScripcie.
 */
public record DictionaryEntry(String value, String label) {

    public static DictionaryEntry from(Dictionary entry) {
        return new DictionaryEntry(entry.name(), entry.label());
    }

    public static <E extends Enum<E> & Dictionary> List<DictionaryEntry> of(Class<E> type) {
        return Arrays.stream(type.getEnumConstants())
                .map(DictionaryEntry::from)
                .toList();
    }
}
