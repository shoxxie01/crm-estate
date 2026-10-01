package pl.delta.crm.inquiry;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.delta.crm.agency.Agency;
import pl.delta.crm.agency.AgencyRepository;
import pl.delta.crm.client.Client;
import pl.delta.crm.client.ClientRepository;
import pl.delta.crm.client.dictionary.Financing;
import pl.delta.crm.client.dictionary.LeadSource;
import pl.delta.crm.client.requirement.ClientRequirementService;
import pl.delta.crm.client.requirement.dto.RequirementRequest;
import pl.delta.crm.client.requirement.dto.RequirementResponse;
import pl.delta.crm.contact.PhoneNumber;
import pl.delta.crm.error.BusinessValidationException;
import pl.delta.crm.error.ClientNotFoundException;
import pl.delta.crm.error.InquiryNotFoundException;
import pl.delta.crm.error.IntakeNotFoundException;
import pl.delta.crm.error.TooManySubmissionsException;
import pl.delta.crm.inquiry.dto.ConvertInquiryRequest;
import pl.delta.crm.inquiry.dto.ConvertInquiryResponse;
import pl.delta.crm.inquiry.dto.InquiryResponse;
import pl.delta.crm.inquiry.dto.IntakeFormResponse;
import pl.delta.crm.inquiry.dto.PublicInquiryRequest;
import pl.delta.crm.inquiry.dto.SaleOfferRequest;
import pl.delta.crm.matching.MatchingService;
import pl.delta.crm.matching.dto.PropertyMatchResponse;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.TransactionType;
import pl.delta.crm.property.dto.DictionaryEntry;
import pl.delta.crm.user.Role;
import pl.delta.crm.user.User;
import tools.jackson.databind.ObjectMapper;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class InquiryService {

    private static final DateTimeFormatter NOTE_DATE =
            DateTimeFormatter.ofPattern("d.MM.yyyy").withZone(ZoneId.of("Europe/Warsaw"));

    private final ClientInquiryRepository inquiries;
    private final AgencyRepository agencies;
    private final ClientRepository clients;
    private final ClientRequirementService requirementService;
    private final MatchingService matching;
    private final IntakeRateLimiter rateLimiter;
    private final ObjectMapper json;

    public InquiryService(ClientInquiryRepository inquiries, AgencyRepository agencies, ClientRepository clients,
                          ClientRequirementService requirementService, MatchingService matching,
                          IntakeRateLimiter rateLimiter, ObjectMapper json) {
        this.inquiries = inquiries;
        this.agencies = agencies;
        this.clients = clients;
        this.requirementService = requirementService;
        this.matching = matching;
        this.rateLimiter = rateLimiter;
        this.json = json;
    }

    // --- strona publiczna ---------------------------------------------------------

    @Transactional(readOnly = true)
    public IntakeFormResponse intakeForm(String token) {
        Agency agency = agencies.findByIntakeToken(token).orElseThrow(IntakeNotFoundException::new);

        return new IntakeFormResponse(
                agency.getName(),
                IntakeConsent.processing(agency),
                IntakeConsent.marketing(agency),
                IntakeConsent.privacyNote(agency),
                DictionaryEntry.of(PropertyType.class),
                DictionaryEntry.of(Financing.class));
    }

    @Transactional
    public void submit(String token, PublicInquiryRequest request, String address) {
        Agency agency = agencies.findByIntakeToken(token).orElseThrow(IntakeNotFoundException::new);

        // Wypełnione pole-pułapka: udajemy sukces, żeby bot nie miał sygnału,
        // że został rozpoznany. I niczego nie zapisujemy.
        if (request.website() != null && !request.website().isBlank()) {
            return;
        }

        if (!rateLimiter.tryAcquire(address)) {
            throw new TooManySubmissionsException();
        }

        InquiryIntent intent = intentOf(request);
        validate(request, intent);

        String payload = intent == InquiryIntent.SELL
                ? json.writeValueAsString(normalized(request.offer()))
                : json.writeValueAsString(request.criteria().forInquiry(null));

        ClientInquiry inquiry = new ClientInquiry(
                agency,
                intent,
                request.firstName().trim(),
                request.lastName().trim(),
                PhoneNumber.normalize(request.phone()),
                normalizeEmail(request.email()),
                payload,
                trimToNull(request.message()),
                IntakeConsent.processing(agency),
                Boolean.TRUE.equals(request.consentMarketing()));

        inquiries.save(inquiry);
    }

    // --- skrzynka zgłoszeń ----------------------------------------------------------

    @Transactional(readOnly = true)
    public List<InquiryResponse> list(InquiryStatus status, User viewer) {
        UUID agencyId = viewer.getAgency().getId();
        return inquiries.findByAgencyIdAndStatusOrderByCreatedAtDesc(agencyId, status).stream()
                .map(inquiry -> toResponse(inquiry, agencyId))
                .toList();
    }

    @Transactional(readOnly = true)
    public long countNew(User viewer) {
        return inquiries.countByAgencyIdAndStatus(viewer.getAgency().getId(), InquiryStatus.NEW);
    }

    /**
     * Oferty pasujące do zgłoszenia kupna jeszcze przed jego przyjęciem. Do
     * rozmowy z klientem. Zgłoszenie sprzedaży nie ma czego dopasowywać do ofert.
     */
    @Transactional(readOnly = true)
    public List<PropertyMatchResponse> matches(UUID id, User viewer) {
        ClientInquiry inquiry = find(id, viewer);
        if (inquiry.getIntent() != InquiryIntent.BUY) {
            return List.of();
        }
        Agency agency = viewer.getAgency();
        return matching.propertiesForDraft(ClientRequirementService.draft(agency, criteria(inquiry)), agency.getId());
    }

    /**
     * Przyjęcie zgłoszenia: nowy klient (źródło „Strona WWW") albo dopięcie do
     * istniejącego. Przy kupnie powstaje poszukiwanie z kryteriów zgłoszenia;
     * przy sprzedaży opis nieruchomości trafia do notatki klienta, a ofertę agent
     * zakłada sam. Ogłoszenie wymaga danych, których formularz nie zbiera.
     */
    @Transactional
    public ConvertInquiryResponse convert(UUID id, ConvertInquiryRequest request, User actor) {
        ClientInquiry inquiry = find(id, actor);
        if (inquiry.getStatus() != InquiryStatus.NEW) {
            throw new BusinessValidationException(Map.of("status", "To zgłoszenie zostało już przyjęte."));
        }

        boolean selling = inquiry.getIntent() == InquiryIntent.SELL;
        String consentNote = "Zgłoszenie z formularza www z " + NOTE_DATE.format(inquiry.getCreatedAt())
                + ". Zgoda na informacje o ofertach: " + (inquiry.isConsentMarketing() ? "tak" : "nie") + ".";
        if (selling) {
            consentNote += "\nChce sprzedać: " + describe(offer(inquiry))
                    + (inquiry.getMessage() == null ? "" : "\nWiadomość: " + inquiry.getMessage());
        }

        Client client;
        if (request != null && request.clientId() != null) {
            client = clients.findByIdAndAgencyId(request.clientId(), actor.getAgency().getId())
                    .orElseThrow(ClientNotFoundException::new);
            // Uzupełniamy tylko brakujące dane. Istniejących nie nadpisujemy tym,
            // co ktoś wpisał w formularz bez weryfikacji.
            if (client.getPhone() == null) {
                client.setPhone(inquiry.getPhone());
            }
            if (client.getEmail() == null) {
                client.setEmail(inquiry.getEmail());
            }
            client.setNotes(client.getNotes() == null ? consentNote : client.getNotes() + "\n\n" + consentNote);
        } else {
            client = new Client(actor.getAgency(), actor, actor, inquiry.getFirstName(), inquiry.getLastName());
            client.setPhone(inquiry.getPhone());
            client.setEmail(inquiry.getEmail());
            client.setSource(LeadSource.WEBSITE);
            client.setNotes(consentNote);
            client = clients.save(client);
        }

        UUID requirementId = null;
        if (!selling) {
            RequirementResponse requirement = requirementService.create(
                    client.getId(), criteria(inquiry).forInquiry(inquiry.getMessage()), actor);
            requirementId = requirement.id();
        }

        inquiry.convert(client, actor);
        return new ConvertInquiryResponse(client.getId(), requirementId);
    }

    /** Odrzucenie usuwa zgłoszenie z danymi osobowymi. Nie ma podstawy, żeby je trzymać. */
    @Transactional
    public void reject(UUID id, User actor) {
        ClientInquiry inquiry = find(id, actor);
        if (inquiry.getStatus() != InquiryStatus.NEW) {
            throw new BusinessValidationException(
                    Map.of("status", "Przyjętego zgłoszenia nie można odrzucić. Jest już częścią karty klienta."));
        }
        inquiries.delete(inquiry);
    }

    // --- link do formularza -----------------------------------------------------------

    @Transactional(readOnly = true)
    public String intakeToken(User viewer) {
        return agencies.findById(viewer.getAgency().getId()).orElseThrow().getIntakeToken();
    }

    @Transactional
    public String regenerateIntakeToken(User actor) {
        if (actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Nowy link do formularza może wygenerować tylko administrator biura.");
        }
        Agency agency = agencies.findById(actor.getAgency().getId()).orElseThrow();
        agency.regenerateIntakeToken();
        return agency.getIntakeToken();
    }

    // --- pomocnicze ---------------------------------------------------------------------

    private ClientInquiry find(UUID id, User viewer) {
        return inquiries.findByIdAndAgencyId(id, viewer.getAgency().getId())
                .orElseThrow(InquiryNotFoundException::new);
    }

    private RequirementRequest criteria(ClientInquiry inquiry) {
        return inquiry.getCriteria() == null ? null : json.readValue(inquiry.getCriteria(), RequirementRequest.class);
    }

    private SaleOfferRequest offer(ClientInquiry inquiry) {
        return inquiry.getOffer() == null ? null : json.readValue(inquiry.getOffer(), SaleOfferRequest.class);
    }

    private static InquiryIntent intentOf(PublicInquiryRequest request) {
        return request.intent() == null ? InquiryIntent.BUY : request.intent();
    }

    private static void validate(PublicInquiryRequest request, InquiryIntent intent) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (trimToNull(request.phone()) == null && trimToNull(request.email()) == null) {
            errors.put("phone", "Podaj telefon lub e-mail. Inaczej biuro nie będzie mogło się z Tobą skontaktować.");
        }

        if (intent == InquiryIntent.SELL) {
            if (request.offer() == null) {
                errors.put("offer", "Opisz nieruchomość, którą chcesz sprzedać.");
            } else if (request.offer().propertyType() != null && request.offer().propertyType().rentOnly()) {
                errors.put("offer.propertyType", "Pokoju nie da się sprzedać osobno. Wybierz inny rodzaj.");
            }
        } else if (request.criteria() == null) {
            errors.put("criteria", "Opisz, czego szukasz.");
        } else if (request.criteria().transactionType() != TransactionType.SALE) {
            // Formularz ma tylko „kupić" i „sprzedać". Najem obsługuje agent.
            errors.put("criteria.transactionType", "Formularz obsługuje tylko kupno albo sprzedaż.");
        } else {
            // Te same reguły co w formularzu agenta; klucze z prefiksem, jak w błędach adnotacji.
            try {
                ClientRequirementService.validate(request.criteria());
            } catch (BusinessValidationException exception) {
                exception.getErrors().forEach((field, message) -> errors.put("criteria." + field, message));
            }
        }

        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }
    }

    /** Przycięte napisy, pusta dzielnica jako brak. */
    private static SaleOfferRequest normalized(SaleOfferRequest offer) {
        return new SaleOfferRequest(offer.propertyType(), offer.city().trim(), trimToNull(offer.district()),
                offer.area(), offer.roomsCount(), offer.expectedPrice());
    }

    /** „Mieszkanie, Łódź. Polesie · 48 m² · pokoje: 2 · oczekiwana cena: 550 000 zł" do notatki klienta. */
    private static String describe(SaleOfferRequest offer) {
        NumberFormat number = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL"));
        number.setMaximumFractionDigits(2);

        List<String> parts = new ArrayList<>();
        parts.add(offer.propertyType().label() + ", " + offer.city()
                + (offer.district() == null ? "" : ". " + offer.district()));
        if (offer.area() != null) {
            parts.add(number.format(offer.area()) + " m²");
        }
        if (offer.roomsCount() != null) {
            parts.add("pokoje: " + offer.roomsCount());
        }
        if (offer.expectedPrice() != null) {
            parts.add("oczekiwana cena: " + number.format(offer.expectedPrice()) + " zł");
        }
        return String.join(" · ", parts);
    }

    private InquiryResponse toResponse(ClientInquiry inquiry, UUID agencyId) {
        Client client = inquiry.getClient();
        User handledBy = inquiry.getHandledBy();

        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getStatus(),
                inquiry.getIntent(),
                inquiry.getFirstName(),
                inquiry.getLastName(),
                inquiry.getPhone(),
                inquiry.getEmail(),
                criteria(inquiry),
                offer(inquiry),
                inquiry.getMessage(),
                inquiry.getConsentProcessingAt(),
                inquiry.getConsentText(),
                inquiry.isConsentMarketing(),
                inquiry.getStatus() == InquiryStatus.NEW ? duplicates(inquiry, agencyId) : List.of(),
                client == null ? null : client.getId(),
                client == null ? null : client.fullName(),
                handledBy == null ? null : handledBy.getFirstName() + " " + handledBy.getLastName(),
                inquiry.getHandledAt(),
                inquiry.getCreatedAt());
    }

    /** Klienci biura z tym samym telefonem albo e-mailem. Kandydaci do dopięcia zamiast nowej karty. */
    private List<InquiryResponse.PossibleDuplicate> duplicates(ClientInquiry inquiry, UUID agencyId) {
        Map<UUID, Client> found = new LinkedHashMap<>();
        Map<UUID, List<String>> reasons = new LinkedHashMap<>();

        if (inquiry.getPhone() != null) {
            for (Client c : clients.findByAgencyIdAndPhone(agencyId, inquiry.getPhone())) {
                found.putIfAbsent(c.getId(), c);
                reasons.computeIfAbsent(c.getId(), key -> new ArrayList<>()).add("telefon");
            }
        }
        if (inquiry.getEmail() != null) {
            for (Client c : clients.findByAgencyIdAndEmail(agencyId, inquiry.getEmail())) {
                found.putIfAbsent(c.getId(), c);
                reasons.computeIfAbsent(c.getId(), key -> new ArrayList<>()).add("e-mail");
            }
        }

        return found.values().stream()
                .map(c -> new InquiryResponse.PossibleDuplicate(
                        c.getId(), c.fullName(), c.getPhone(), c.getEmail(), reasons.get(c.getId())))
                .toList();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeEmail(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
