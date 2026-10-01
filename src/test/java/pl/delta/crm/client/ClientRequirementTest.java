package pl.delta.crm.client;

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
import pl.delta.crm.PostgresTestcontainerConfig;
import pl.delta.crm.calendar.CalendarEventRepository;
import pl.delta.crm.client.requirement.ClientRequirementRepository;
import pl.delta.crm.property.PropertyRepository;
import pl.delta.crm.user.UserRepository;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Poszukiwania klientów.
 *
 * <p>Najważniejsze do upilnowania: rola kupującego / najemcy liczy się
 * wyłącznie z aktywnych poszukiwań, zapis nie gubi kolekcji (rodzaje,
 * lokalizacje w kolejności, cechy z podziałem na konieczne i mile widziane),
 * reguły między polami działają, a poszukiwanie cudzego klienta jest
 * niewidoczne.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfig.class)
class ClientRequirementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @BeforeEach
    void clearData() {
        clearAll();
    }

    @AfterEach
    void cleanUp() {
        clearAll();
    }

    private void clearAll() {
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

    private String createClient(String token) throws Exception {
        String body = mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Piotr",
                                  "lastName": "Nowak",
                                  "phone": "605 405 932"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("id").asText();
    }

    private static String purchaseBody() {
        return """
                {
                  "transactionType": "SALE",
                  "propertyTypes": ["HOUSE", "APARTMENT"],
                  "marketType": "SECONDARY",
                  "locations": [
                    { "city": "Łódź", "district": "Polesie" },
                    { "city": " Łódź ", "district": "polesie" },
                    { "city": "Zgierz", "district": " " }
                  ],
                  "priceMin": 450000,
                  "priceMax": 600000,
                  "areaMin": 45,
                  "roomsMin": 2,
                  "roomsMax": 3,
                  "floorMin": 1,
                  "excludeTopFloor": true,
                  "financing": "MORTGAGE_APPROVED",
                  "moveInDate": "2026-12-01",
                  "requiredFeatures": ["BALCONY", "ELEVATOR"],
                  "preferredFeatures": ["BASEMENT", "ELEVATOR"],
                  "notes": "Dziecko w drodze, chcą się przeprowadzić przed zimą."
                }
                """;
    }

    private String createRequirement(String token, String clientId, String body) throws Exception {
        String response = mockMvc.perform(post("/api/clients/" + clientId + "/requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    // --- testy ---------------------------------------------------------------

    @Test
    @DisplayName("poszukiwanie zapisuje kryteria wraz z kolekcjami")
    void createsRequirementWithCollections() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String clientId = createClient(token);

        mockMvc.perform(post("/api/clients/" + clientId + "/requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                // Kolejność słownika, nie kolejność z żądania.
                .andExpect(jsonPath("$.propertyTypes[0]").value("APARTMENT"))
                .andExpect(jsonPath("$.propertyTypes[1]").value("HOUSE"))
                // Duplikat (inna wielkość liter, spacje) wypada, pusta dzielnica to null.
                .andExpect(jsonPath("$.locations.length()").value(2))
                .andExpect(jsonPath("$.locations[0].city").value("Łódź"))
                .andExpect(jsonPath("$.locations[1].city").value("Zgierz"))
                .andExpect(jsonPath("$.locations[1].district").doesNotExist())
                .andExpect(jsonPath("$.priceMax").value(600000))
                .andExpect(jsonPath("$.areaMax").doesNotExist())
                .andExpect(jsonPath("$.excludeTopFloor").value(true))
                .andExpect(jsonPath("$.financing").value("MORTGAGE_APPROVED"))
                // Cecha w obu zbiorach zostaje wyłącznie konieczna.
                .andExpect(jsonPath("$.requiredFeatures.length()").value(2))
                .andExpect(jsonPath("$.preferredFeatures.length()").value(1))
                .andExpect(jsonPath("$.preferredFeatures[0]").value("BASEMENT"));

        // Karta klienta niesie poszukiwanie i wyliczoną rolę.
        mockMvc.perform(get("/api/clients/" + clientId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements.length()").value(1))
                .andExpect(jsonPath("$.requirements[0].locations[0].district").value("Polesie"))
                .andExpect(jsonPath("$.buyerCount").value(1))
                .andExpect(jsonPath("$.tenantCount").value(0));
    }

    @Test
    @DisplayName("rola kupującego / najemcy liczy się tylko z aktywnych poszukiwań")
    void rolesCountOnlyActiveRequirements() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String clientId = createClient(token);

        String purchaseId = createRequirement(token, clientId, purchaseBody());
        createRequirement(token, clientId, """
                { "transactionType": "RENT", "propertyTypes": ["GARAGE"] }
                """);

        mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].buyerCount").value(1))
                .andExpect(jsonPath("$.content[0].tenantCount").value(1));

        mockMvc.perform(put("/api/clients/" + clientId + "/requirements/" + purchaseId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"FULFILLED\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));

        mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].buyerCount").value(0))
                .andExpect(jsonPath("$.content[0].tenantCount").value(1));

        // Zrealizowane zostaje na karcie. Jako historia, za aktywnymi.
        mockMvc.perform(get("/api/clients/" + clientId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.requirements.length()").value(2))
                .andExpect(jsonPath("$.requirements[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.requirements[1].status").value("FULFILLED"));
    }

    @Test
    @DisplayName("edycja podmienia kolekcje, a przy najmie czyści finansowanie")
    void updateReplacesCollectionsAndClearsFinancingForRent() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String clientId = createClient(token);
        String id = createRequirement(token, clientId, purchaseBody());

        mockMvc.perform(put("/api/clients/" + clientId + "/requirements/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType": "RENT",
                                  "propertyTypes": ["ROOM"],
                                  "locations": [{ "city": "Kraków" }],
                                  "priceMax": 1800,
                                  "financing": "CASH",
                                  "preferredFeatures": ["PETS_ALLOWED"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("RENT"))
                .andExpect(jsonPath("$.propertyTypes.length()").value(1))
                .andExpect(jsonPath("$.locations.length()").value(1))
                .andExpect(jsonPath("$.locations[0].city").value("Kraków"))
                .andExpect(jsonPath("$.financing").doesNotExist())
                .andExpect(jsonPath("$.priceMin").doesNotExist())
                .andExpect(jsonPath("$.requiredFeatures.length()").value(0))
                .andExpect(jsonPath("$.preferredFeatures[0]").value("PETS_ALLOWED"))
                // Status pominięty w żądaniu = bez zmian.
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("reguły między polami: zakresy, pokój tylko na najem, co najmniej jeden rodzaj")
    void rejectsInconsistentCriteria() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String clientId = createClient(token);

        mockMvc.perform(post("/api/clients/" + clientId + "/requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType": "SALE",
                                  "propertyTypes": ["ROOM"],
                                  "priceMin": 700000,
                                  "priceMax": 500000,
                                  "floorMin": 4,
                                  "floorMax": 2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.propertyTypes").isNotEmpty())
                .andExpect(jsonPath("$.errors.priceMax").isNotEmpty())
                .andExpect(jsonPath("$.errors.floorMax").isNotEmpty());

        mockMvc.perform(post("/api/clients/" + clientId + "/requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType": "SALE",
                                  "propertyTypes": [],
                                  "locations": [{ "city": " " }]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.propertyTypes").isNotEmpty())
                .andExpect(jsonPath("$.errors['locations[0].city']").isNotEmpty());

        assertThat(requirements.count()).isZero();
    }

    @Test
    @DisplayName("poszukiwania klienta innego biura są niewidoczne i dają 404")
    void isolatesAgencies() throws Exception {
        String ourToken = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String clientId = createClient(ourToken);
        String id = createRequirement(ourToken, clientId, purchaseBody());

        String otherToken = tokenFor("jan@omega.pl", "Omega Nieruchomości");

        mockMvc.perform(get("/api/clients/" + clientId + "/requirements")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/clients/" + clientId + "/requirements/" + id + "/status")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"CLOSED\" }"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/clients/" + clientId + "/requirements/" + id)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        assertThat(requirements.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("usunięcie klienta usuwa jego poszukiwania")
    void deletingClientRemovesRequirements() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String clientId = createClient(token);
        createRequirement(token, clientId, purchaseBody());

        mockMvc.perform(delete("/api/clients/" + clientId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(requirements.count()).isZero();
    }
}
