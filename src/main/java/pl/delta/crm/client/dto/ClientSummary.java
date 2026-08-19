package pl.delta.crm.client.dto;

import pl.delta.crm.client.Client;
import pl.delta.crm.client.dictionary.ClientStatus;
import pl.delta.crm.client.dictionary.LeadSource;

import java.time.Instant;
import java.util.UUID;

/**
 * Wiersz listy klientów. {@code sellCount} i {@code rentCount} to liczba
 * powierzonych ofert na sprzedaż i na wynajem — front wyprowadza z nich etykietę
 * „sprzedający / wynajmujący / oba / brak zlecenia". Rozróżnienie nie jest polem
 * klienta, tylko pochodną jego ofert.
 */
public record ClientSummary(
        UUID id,
        String firstName,
        String lastName,
        String phone,
        String email,
        LeadSource source,
        ClientStatus status,
        String agentName,
        long sellCount,
        long rentCount,
        Instant createdAt
) {

    public static ClientSummary from(Client client, long sellCount, long rentCount) {
        return new ClientSummary(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getPhone(),
                client.getEmail(),
                client.getSource(),
                client.getStatus(),
                client.getAgent().getFirstName() + " " + client.getAgent().getLastName(),
                sellCount,
                rentCount,
                client.getCreatedAt()
        );
    }
}
