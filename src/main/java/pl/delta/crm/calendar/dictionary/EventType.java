package pl.delta.crm.calendar.dictionary;

import pl.delta.crm.property.dictionary.Dictionary;

/**
 * Rodzaj zdarzenia w kalendarzu biura.
 *
 * <p>Lista jest celowo krótka i zamknięta. Po typie filtruje się widok i z
 * niego biorą się kolory w siatce, a słownik puchnący do trzydziestu pozycji
 * przestaje cokolwiek segregować. Nietypowe terminy idą jako {@link #OTHER}
 * z opisem.
 */
public enum EventType implements Dictionary {

    /** Pokazanie oferty osobie zainteresowanej. Najczęstszy wpis w biurze. */
    PRESENTATION("Prezentacja"),

    /** Spotkanie z właścicielem, inwestorem, w biurze. Bez pokazywania obiektu. */
    MEETING("Spotkanie"),

    /** Oględziny z wyceną, zwykle przed przyjęciem oferty. */
    VALUATION("Wycena"),

    /** Umowa pośrednictwa, przedwstępna, akt u notariusza. */
    CONTRACT_SIGNING("Podpisanie umowy"),

    /** Otwarta prezentacja dla wielu osób w jednym oknie czasowym. */
    OPEN_HOUSE("Dzień otwarty"),

    /** Sesja zdjęciowa / wideo. Bez zdjęć oferta nie przejdzie na portal. */
    PHOTO_SESSION("Sesja zdjęciowa"),

    /** Umówiony telefon. Kontakt do wykonania o konkretnej porze. */
    PHONE_CALL("Telefon"),

    /** Czynność bez drugiej strony: dokumenty, wypis z rejestru, opis oferty. */
    TASK("Zadanie"),

    OTHER("Inne");

    private final String label;

    EventType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
