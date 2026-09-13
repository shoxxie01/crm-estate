package pl.delta.crm.client.dto;

import pl.delta.crm.client.Client;
import pl.delta.crm.client.dictionary.ClientStatus;
import pl.delta.crm.client.dictionary.LeadSource;

import java.time.Instant;
import java.util.UUID;

/**
 * Wiersz listy klientów. {@code sellCount} i {@code rentCount} to liczba
 * powierzonych ofert na sprzedaż i na wynajem, {@code buyerCount} i
 * {@code tenantCount} — liczba aktywnych poszukiwań kupna i najmu. Front
 * wyprowadza z nich etykiety ról; żadna z ról nie jest polem klienta.
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
        long buyerCount,
        long tenantCount,
        Instant createdAt
) {

    public static ClientSummary from(Client client, long[] offerCounts, long[] requirementCounts) {
        return new ClientSummary(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getPhone(),
                client.getEmail(),
                client.getSource(),
                client.getStatus(),
                client.getAgent().getFirstName() + " " + client.getAgent().getLastName(),
                offerCounts[0],
                offerCounts[1],
                requirementCounts[0],
                requirementCounts[1],
                client.getCreatedAt()
        );
    }
}
