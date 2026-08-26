package pl.delta.crm.calendar.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pl.delta.crm.calendar.dictionary.EventOutcome;
import pl.delta.crm.calendar.dictionary.EventStatus;
import pl.delta.crm.calendar.dictionary.EventType;

import java.time.Instant;
import java.util.UUID;

/**
 * Formularz terminu — ten sam przy dodawaniu i edycji, jak w pozostałych modułach.
 *
 * <p>Wymagane są tylko rodzaj i zakres czasu. <b>Tytuł jest opcjonalny</b>: przy
 * powiązanej ofercie serwis złoży go z rodzaju i adresu („Prezentacja —
 * Grzybowska 41"), bo przepisywanie tego ręcznie przy każdym terminie to praca,
 * której komputer może nie zlecać człowiekowi.
 *
 * <p>Reguły dotyczące dwóch pól naraz — koniec po początku, rezultat tylko przy
 * statusie {@code COMPLETED}, oferta i klient z tego samego biura — pilnuje
 * serwis, bo Bean Validation widzi pojedyncze pola.
 */
public record CreateEventRequest(

        @NotNull(message = "Wybierz rodzaj terminu.")
        EventType type,

        EventStatus status,

        @Size(max = 120, message = "Tytuł jest zbyt długi.")
        String title,

        @Size(max = 5_000, message = "Opis jest zbyt długi.")
        String description,

        @Size(max = 200, message = "Miejsce jest zbyt długie.")
        String location,

        @NotNull(message = "Podaj początek terminu.")
        Instant startsAt,

        @NotNull(message = "Podaj koniec terminu.")
        Instant endsAt,

        /**
         * Obiekt, nie prymityw: przy {@code boolean} pominięcie pola w ciele
         * żądania wywraca całą deserializację rekordu, a wtedy nawet poprawny
         * termin wraca jako „nie udało się odczytać danych" — mimo że pole jest
         * opcjonalne. Brak wartości czytamy jako {@code false}.
         */
        Boolean allDay,

        /** Oferta, której dotyczy termin. Pusta przy spotkaniu bez oferty. */
        UUID propertyId,

        /** Właściciel z bazy klientów. Pusty, gdy termin jest z kupującym. */
        UUID clientId,

        /**
         * Druga strona spoza bazy klientów — kupujący, najemca, rzeczoznawca.
         * Patrz komentarz w encji: strony popytu nie ma jeszcze w modelu.
         */
        @Size(max = 160, message = "Nazwa jest zbyt długa.")
        String counterpartyName,

        // Ten sam format co przy kliencie — spójność między modułami.
        // Normalizacją zajmuje się PhoneNumber, tak samo po obu stronach.
        @Pattern(
                regexp = "^\\+?\\d(?:[ -]?\\d){8,14}$",
                message = "Podaj numer telefonu, np. +48 605 405 932.")
        @Size(max = 30, message = "Numer telefonu jest zbyt długi.")
        String counterpartyPhone,

        /** Czyj to termin. Pusty = osoba dodająca wpis. */
        UUID agentId,

        EventOutcome outcome,

        @Size(max = 5_000, message = "Notatka jest zbyt długa.")
        String outcomeNote
) {
}
