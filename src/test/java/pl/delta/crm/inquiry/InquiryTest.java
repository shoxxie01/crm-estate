package pl.delta.crm.inquiry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pl.delta.crm.PostgresTestcontainerConfig;
import pl.delta.crm.calendar.CalendarEventRepository;
import pl.delta.crm.client.ClientRepository;
import pl.delta.crm.client.requirement.ClientRequirementRepository;
import pl.delta.crm.property.PropertyRepository;
import pl.delta.crm.user.UserRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Publiczny formularz zgłoszeniowy i skrzynka zgłoszeń.
 *
 * <p>Każdy test wysyła zgłoszenia z innego adresu IP — limit zgłoszeń jest
 * w pamięci procesu i przeżywa między testami tej samej klasy.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfig.class)
class InquiryTest {

    private static final AtomicInteger NEXT_ADDRESS = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientInquiryRepository inquiries;

    @Autowired
    private ClientRequirementRepository requirements;

    @Autowired
    private CalendarEventRepository events;

    @Autowired
    private PropertyRepository properties;

    @Autowired
    private ClientRepository clients;

    @Autowired
    private UserRepository users;

    private String token;
    private String intakeToken;
    private String address;

    @BeforeEach
    void setUp() throws Exception {
        clearAll();
        token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        intakeToken = intakeTokenFor(token);
        address = "10.0.0." + NEXT_ADDRESS.getAndIncrement();
    }

    @AfterEach
    void cleanUp() {
        clearAll();
    }

    private void clearAll() {
        inquiries.deleteAll();
        requirements.deleteAll();
        events.deleteAll();
        properties.deleteAll();
        clients.deleteAll();
        users.deleteAll();
    }

    // --- dane testowe --------------------------------------------------------

    private String tokenFor(String email, String agency) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Anna",
                                  "lastName": "Kowalska",
                                  "email": "%s",
                                  "password": "TajneHaslo123!",
                                  "agencyName": "%s"
                                }
                                """.formatted(email, agency)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private String intakeTokenFor(String authToken) throws Exception {
        String body = mockMvc.perform(get("/api/inquiries/intake-link")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private static String inquiryBody(String phone, String extra) {
        return """
                {
                  "firstName": "Piotr",
                  "lastName": "Nowak",
                  "phone": "%s",
                  "email": "Piotr.Nowak@example.com",
                  "criteria": {
                    "transactionType": "SALE",
                    "propertyTypes": ["APARTMENT"],
                    "locations": [{ "city": "Łódź", "district": "Polesie" }],
                    "priceMax": 600000,
                    "roomsMin": 2,
                    "financing": "MORTGAGE_APPROVED"
                  },
                  "message": "Najlepiej blisko parku.",
                  "consentProcessing": true,
                  "consentMarketing": true%s
                }
                """.formatted(phone, extra);
    }

    private MockHttpServletRequestBuilder submit(String body) {
        return post("/api/public/intake/" + intakeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                });
    }

    private String firstInquiryId() throws Exception {
        String body = mockMvc.perform(get("/api/inquiries")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get(0).get("id").asText();
    }

    // --- testy ---------------------------------------------------------------

    @Test
    @DisplayName("formularz działa bez logowania i zdradza tylko nazwę biura i zgody")
    void intakeFormIsPublic() throws Exception {
        mockMvc.perform(get("/api/public/intake/" + intakeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agencyName").value("Delta Nieruchomości"))
                .andExpect(jsonPath("$.consentProcessingText").value(containsString("Delta Nieruchomości")))
                .andExpect(jsonPath("$.propertyType.length()").value(7));

        mockMvc.perform(get("/api/public/intake/nieistniejacy-klucz"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("zgłoszenie trafia do skrzynki z dowodem zgody, nie do klientów")
    void submissionLandsInInbox() throws Exception {
        mockMvc.perform(submit(inquiryBody("605 405 932", "")))
                .andExpect(status().isNoContent());

        assertThat(clients.count()).isZero();

        mockMvc.perform(get("/api/inquiries/count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.new").value(1));

        mockMvc.perform(get("/api/inquiries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].phone").value("+48 605 405 932"))
                .andExpect(jsonPath("$[0].email").value("piotr.nowak@example.com"))
                .andExpect(jsonPath("$[0].criteria.locations[0].district").value("Polesie"))
                .andExpect(jsonPath("$[0].consentText").value(containsString("przetwarzanie")))
                .andExpect(jsonPath("$[0].consentMarketing").value(true))
                .andExpect(jsonPath("$[0].possibleDuplicates.length()").value(0));
    }

    @Test
    @DisplayName("bez zgody, bez kontaktu albo z błędnymi kryteriami zgłoszenie jest odrzucane")
    void rejectsInvalidSubmission() throws Exception {
        mockMvc.perform(submit("""
                        {
                          "firstName": "Piotr",
                          "lastName": "Nowak",
                          "criteria": {
                            "transactionType": "SALE",
                            "propertyTypes": ["ROOM"],
                            "priceMin": 700000,
                            "priceMax": 500000
                          },
                          "consentProcessing": false
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.consentProcessing").isNotEmpty());

        mockMvc.perform(submit("""
                        {
                          "firstName": "Piotr",
                          "lastName": "Nowak",
                          "criteria": {
                            "transactionType": "SALE",
                            "propertyTypes": ["ROOM"],
                            "priceMin": 700000,
                            "priceMax": 500000
                          },
                          "consentProcessing": true
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").isNotEmpty())
                .andExpect(jsonPath("$.errors['criteria.propertyTypes']").isNotEmpty())
                .andExpect(jsonPath("$.errors['criteria.priceMax']").isNotEmpty());

        assertThat(inquiries.count()).isZero();
    }

    @Test
    @DisplayName("wypełnione pole-pułapka udaje sukces i niczego nie zapisuje")
    void honeypotSilentlyDropsBots() throws Exception {
        mockMvc.perform(submit(inquiryBody("605 405 932", ", \"website\": \"http://spam.example\"")))
                .andExpect(status().isNoContent());

        assertThat(inquiries.count()).isZero();
    }

    @Test
    @DisplayName("limit zgłoszeń z jednego adresu")
    void limitsSubmissionsPerAddress() throws Exception {
        for (int i = 0; i < IntakeRateLimiter.MAX_SUBMISSIONS; i++) {
            mockMvc.perform(submit(inquiryBody("605 405 93" + i, "")))
                    .andExpect(status().isNoContent());
        }

        mockMvc.perform(submit(inquiryBody("605 405 939", "")))
                .andExpect(status().isTooManyRequests());

        assertThat(inquiries.count()).isEqualTo(IntakeRateLimiter.MAX_SUBMISSIONS);
    }

    @Test
    @DisplayName("przyjęcie tworzy klienta ze źródłem „Strona WWW” i aktywnym poszukiwaniem")
    void convertCreatesClientWithRequirement() throws Exception {
        mockMvc.perform(submit(inquiryBody("605 405 932", ""))).andExpect(status().isNoContent());
        String inquiryId = firstInquiryId();

        String body = mockMvc.perform(post("/api/inquiries/" + inquiryId + "/convert")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String clientId = objectMapper.readTree(body).get("clientId").asText();

        mockMvc.perform(get("/api/clients/" + clientId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Piotr"))
                .andExpect(jsonPath("$.source").value("WEBSITE"))
                .andExpect(jsonPath("$.notes").value(containsString("Zgoda na informacje o ofertach: tak")))
                .andExpect(jsonPath("$.buyerCount").value(1))
                .andExpect(jsonPath("$.requirements[0].financing").value("MORTGAGE_APPROVED"))
                .andExpect(jsonPath("$.requirements[0].notes").value("Najlepiej blisko parku."));

        // Przyjęte znika z nowych i nie da się go przyjąć drugi raz.
        mockMvc.perform(get("/api/inquiries/count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.new").value(0));
        mockMvc.perform(get("/api/inquiries").param("status", "CONVERTED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].clientId").value(clientId))
                .andExpect(jsonPath("$[0].handledByName").value("Anna Kowalska"));
        mockMvc.perform(post("/api/inquiries/" + inquiryId + "/convert")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("zgłoszenie znanej osoby podpowiada duplikat i daje się do niej dopiąć")
    void convertAttachesToExistingClient() throws Exception {
        String existing = objectMapper.readTree(mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "firstName": "Piotr", "lastName": "Nowak", "phone": "+48 605 405 932" }
                                """))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(submit(inquiryBody("605405932", ""))).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/inquiries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].possibleDuplicates[0].id").value(existing))
                .andExpect(jsonPath("$[0].possibleDuplicates[0].matchedBy[0]").value("telefon"));

        mockMvc.perform(post("/api/inquiries/" + firstInquiryId() + "/convert")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"clientId\": \"%s\" }".formatted(existing)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(existing));

        assertThat(clients.count()).isEqualTo(1);
        mockMvc.perform(get("/api/clients/" + existing)
                        .header("Authorization", "Bearer " + token))
                // Brakujący e-mail uzupełniony z formularza, telefon nietknięty.
                .andExpect(jsonPath("$.email").value("piotr.nowak@example.com"))
                .andExpect(jsonPath("$.requirements.length()").value(1));
    }

    @Test
    @DisplayName("odrzucenie usuwa zgłoszenie; cudze biuro go nie widzi")
    void rejectDeletesAndAgenciesAreIsolated() throws Exception {
        mockMvc.perform(submit(inquiryBody("605 405 932", ""))).andExpect(status().isNoContent());
        String inquiryId = firstInquiryId();

        String otherToken = tokenFor("jan@omega.pl", "Omega Nieruchomości");
        mockMvc.perform(get("/api/inquiries")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(delete("/api/inquiries/" + inquiryId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/inquiries/" + inquiryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(inquiries.count()).isZero();
    }

    @Test
    @DisplayName("zgłoszenie sprzedaży: opis nieruchomości w skrzynce, po przyjęciu klient z notatką, bez poszukiwania")
    void sellInquiryBecomesClientWithNote() throws Exception {
        mockMvc.perform(submit("""
                        {
                          "intent": "SELL",
                          "firstName": "Ewa",
                          "lastName": "Wiśniewska",
                          "phone": "601 202 303",
                          "offer": {
                            "propertyType": "APARTMENT",
                            "city": " Łódź ",
                            "district": " ",
                            "area": 48.5,
                            "roomsCount": 2,
                            "expectedPrice": 550000
                          },
                          "message": "Mieszkanie po remoncie.",
                          "consentProcessing": true
                        }
                        """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/inquiries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].intent").value("SELL"))
                .andExpect(jsonPath("$[0].offer.city").value("Łódź"))
                .andExpect(jsonPath("$[0].offer.district").doesNotExist())
                .andExpect(jsonPath("$[0].criteria").doesNotExist());

        String inquiryId = firstInquiryId();
        mockMvc.perform(get("/api/inquiries/" + inquiryId + "/matches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));

        String body = mockMvc.perform(post("/api/inquiries/" + inquiryId + "/convert")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirementId").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/clients/" + objectMapper.readTree(body).get("clientId").asText())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.notes").value(containsString("Chce sprzedać: Mieszkanie, Łódź")))
                .andExpect(jsonPath("$.notes").value(containsString("oczekiwana cena: 550")))
                .andExpect(jsonPath("$.notes").value(containsString("Mieszkanie po remoncie.")))
                .andExpect(jsonPath("$.requirements.length()").value(0));
    }

    @Test
    @DisplayName("formularz przyjmuje tylko kupno albo sprzedaż — bez najmu i bez brakujących danych")
    void onlyBuyOrSell() throws Exception {
        mockMvc.perform(submit("""
                        {
                          "firstName": "Piotr",
                          "lastName": "Nowak",
                          "phone": "605 405 932",
                          "criteria": { "transactionType": "RENT", "propertyTypes": ["APARTMENT"] },
                          "consentProcessing": true
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['criteria.transactionType']").isNotEmpty());

        mockMvc.perform(submit("""
                        {
                          "intent": "SELL",
                          "firstName": "Piotr",
                          "lastName": "Nowak",
                          "phone": "605 405 932",
                          "consentProcessing": true
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.offer").isNotEmpty());

        assertThat(inquiries.count()).isZero();
    }

    @Test
    @DisplayName("nowy link unieważnia stary")
    void regeneratedLinkInvalidatesOldOne() throws Exception {
        String old = intakeToken;

        String body = mockMvc.perform(post("/api/inquiries/intake-link/regenerate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String fresh = objectMapper.readTree(body).get("token").asText();

        assertThat(fresh).isNotEqualTo(old);
        mockMvc.perform(get("/api/public/intake/" + old)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/intake/" + fresh)).andExpect(status().isOk());
    }
}
