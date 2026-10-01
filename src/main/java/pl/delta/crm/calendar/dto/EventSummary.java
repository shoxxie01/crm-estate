package pl.delta.crm.calendar.dto;

import pl.delta.crm.calendar.CalendarEvent;
import pl.delta.crm.calendar.dictionary.EventOutcome;
import pl.delta.crm.calendar.dictionary.EventStatus;
import pl.delta.crm.calendar.dictionary.EventType;
import pl.delta.crm.client.Client;
import pl.delta.crm.deal.Deal;
import pl.delta.crm.property.Address;
import pl.delta.crm.property.Property;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafelek w siatce kalendarza i wiersz listy terminów.
 *
 * <p>Struktura jest płaska, bo widok miesiąca renderuje kilkadziesiąt takich
 * pozycji naraz i nie ma po co wysyłać do niego zagnieżdżonych ofert. Nazwy
 * powiązań są rozwinięte po stronie serwera. Front nie dociąga oferty ani
 * klienta tylko po to, żeby napisać, czego dotyczy termin.
 */
public record EventSummary(
        UUID id,
        EventType type,
        EventStatus status,
        EventOutcome outcome,
        String title,
        Instant startsAt,
        Instant endsAt,
        boolean allDay,

        /** Miejsce do pokazania. Własne albo wyprowadzone z adresu oferty. */
        String location,

        UUID agentId,
        String agentName,

        UUID propertyId,
        String propertyReference,
        String propertyTitle,

        UUID clientId,
        String clientName,

        UUID dealId,
        String dealTitle,

        String counterpartyName,
        String counterpartyPhone
) {

    public static EventSummary from(CalendarEvent event) {
        Property property = event.getProperty();
        Client client = event.getClient();
        Deal deal = event.getDeal();

        return new EventSummary(
                event.getId(),
                event.getType(),
                event.getStatus(),
                event.getOutcome(),
                event.getTitle(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.isAllDay(),
                displayLocation(event),
                event.getAgent().getId(),
                event.getAgent().getFirstName() + " " + event.getAgent().getLastName(),
                property == null ? null : property.getId(),
                property == null ? null : property.getReferenceNumber(),
                property == null ? null : property.getTitle(),
                client == null ? null : client.getId(),
                client == null ? null : client.fullName(),
                deal == null ? null : deal.getId(),
                deal == null ? null : deal.getTitle(),
                event.getCounterpartyName(),
                event.getCounterpartyPhone()
        );
    }

    /**
     * Przy powiązanej ofercie miejsce spotkania jest po prostu jej adresem.
     * Przepisywanie go ręcznie w termin oznaczałoby dwa źródła prawdy i adres,
     * który zostaje nieaktualny po korekcie oferty. Własne pole wygrywa, bo
     * bywa dokładniejsze („wejście od podwórza", biuro, kancelaria).
     */
    private static String displayLocation(CalendarEvent event) {
        if (event.getLocation() != null) {
            return event.getLocation();
        }
        // Termin nie musi mieć oferty. Telefon czy zadanie własne nie dotyczy
        // żadnej. Wtedy po prostu nie ma czego pokazać jako miejsca.
        Property property = event.getProperty();
        if (property == null) {
            return null;
        }

        Address address = property.getAddress();
        return address == null ? null : address.shortLine();
    }
}
