package pl.delta.crm.inquiry.dto;

import pl.delta.crm.client.requirement.dto.RequirementRequest;
import pl.delta.crm.inquiry.InquiryIntent;
import pl.delta.crm.inquiry.InquiryStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Zgłoszenie w skrzynce, z podpowiedzią, czy ta osoba nie jest już klientem. */
public record InquiryResponse(
        UUID id,
        InquiryStatus status,
        InquiryIntent intent,
        String firstName,
        String lastName,
        String phone,
        String email,
        /** Tylko przy kupnie. */
        RequirementRequest criteria,
        /** Tylko przy sprzedaży. */
        SaleOfferRequest offer,
        String message,
        Instant consentProcessingAt,
        String consentText,
        boolean consentMarketing,
        List<PossibleDuplicate> possibleDuplicates,
        UUID clientId,
        String clientName,
        String handledByName,
        Instant handledAt,
        Instant createdAt
) {

    /** Klient z tym samym telefonem albo e-mailem; {@code matchedBy} to „telefon" / „e-mail". */
    public record PossibleDuplicate(UUID id, String name, String phone, String email, List<String> matchedBy) {
    }
}
