package pl.delta.crm.matching;

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
import pl.delta.crm.client.ClientRepository;
import pl.delta.crm.client.requirement.ClientRequirementRepository;
import pl.delta.crm.property.PropertyRepository;
import pl.delta.crm.user.UserRepository;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dopasowanie ofert do poszukiwań w obie strony.
 *
 * <p>Oferta testowa: mieszkanie na sprzedaż, Łódź–Polesie, 549 000 zł, 47,5 m²,
 * 2 pokoje, 3. piętro z 4, z balkonem i piwnicą. Poszukiwania ustawiamy wokół
 * niej tak, żeby każde sprawdzało jedną regułę.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfig.class)
class MatchingTest {

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

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        clearAll();
        token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
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

    private String createProperty(String status) throws Exception {
        return createProperty(status,
                "{ \"totalArea\": 47.50, \"roomsCount\": 2, \"floorNo\": 3, \"buildingFloorsCount\": 4 }");
    }

    private String createProperty(String status, String area) throws Exception {
        String body = mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "propertyType": "APARTMENT",
                                  "transactionType": "SALE",
                                  "marketType": "SECONDARY",
                                  "status": "%s",
                                  "title": "Dwupokojowe z balkonem, Stare Polesie",
                                  "description": "Mieszkanie po remoncie, druga linia zabudowy, cicho.",
                                  "pricing": { "price": 549000.00, "priceCurrency": "PLN" },
                                  "area": %s,
                                  "address": {
                                    "voivodeship": "LODZKIE",
                                    "county": "Łódź",
                                    "city": "Łódź",
                                    "district": "Polesie",
                                    "street": "Gdańska",
                                    "postalCode": "90-001"
                                  },
                                  "building": {
                                    "buildYear": 1938,
                                    "buildingType": "TENEMENT",
                                    "buildingMaterial": "BRICK",
                                    "constructionStatus": "READY_TO_MOVE_IN",
                                    "ownershipForm": "FREEHOLD"
                                  },
                                  "energy": { "energyPrimary": 132.40, "energyClass": "D" },
                                  "heatingTypes": ["DISTRICT"],
                                  "features": ["BALCONY", "BASEMENT"]
                                }
                                """.formatted(status, area)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String createClient(String lastName) throws Exception {
        String body = mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "firstName": "Jan", "lastName": "%s", "phone": "605 405 932" }
                                """.formatted(lastName)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String createRequirement(String clientId, String criteria) throws Exception {
        String body = mockMvc.perform(post("/api/clients/" + clientId + "/requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(criteria))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    // --- testy ---------------------------------------------------------------

    @Test
    @DisplayName("karta oferty pokazuje klientów, których poszukiwanie pasuje — z wyjaśnieniem")
    void listsMatchingClientsForProperty() throws Exception {
        String propertyId = createProperty("DRAFT");

        // Pasuje w pełni: bez polskich znaków w nazwie miasta, cechy konieczne są.
        String exact = createClient("Dokładny");
        createRequirement(exact, """
                {
                  "transactionType": "SALE",
                  "propertyTypes": ["APARTMENT", "HOUSE"],
                  "locations": [{ "city": "lodz", "district": "polesie" }],
                  "priceMax": 600000,
                  "areaMin": 40,
                  "roomsMin": 2,
                  "floorMin": 1,
                  "excludeTopFloor": true,
                  "requiredFeatures": ["BALCONY"],
                  "preferredFeatures": ["BASEMENT", "ELEVATOR"]
                }
                """);

        // Cena ~5,6% ponad budżet — pasuje z ostrzeżeniem.
        String near = createClient("Prawie");
        createRequirement(near, """
                { "transactionType": "SALE", "propertyTypes": ["APARTMENT"], "priceMax": 520000 }
                """);

        // Nie pasują: inne miasto, za drogo, brak windy, najem, wstrzymane.
        createRequirement(createClient("Krakow"), """
                { "transactionType": "SALE", "propertyTypes": ["APARTMENT"], "locations": [{ "city": "Kraków" }] }
                """);
        createRequirement(createClient("Budzet"), """
                { "transactionType": "SALE", "propertyTypes": ["APARTMENT"], "priceMax": 450000 }
                """);
        createRequirement(createClient("Winda"), """
                { "transactionType": "SALE", "propertyTypes": ["APARTMENT"], "requiredFeatures": ["ELEVATOR"] }
                """);
        createRequirement(createClient("Najem"), """
                { "transactionType": "RENT", "propertyTypes": ["APARTMENT"] }
                """);
        createRequirement(createClient("Wstrzymany"), """
                { "transactionType": "SALE", "propertyTypes": ["APARTMENT"], "status": "PAUSED" }
                """);

        mockMvc.perform(get("/api/properties/" + propertyId + "/matches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Pewne dopasowanie przed tym z ostrzeżeniem.
                .andExpect(jsonPath("$[0].clientId").value(exact))
                .andExpect(jsonPath("$[0].warnings").value(0))
                .andExpect(jsonPath("$[0].phone").value("+48 605 405 932"))
                .andExpect(jsonPath("$[0].criteria[?(@.criterion == 'LOCATION')].verdict").value("MET"))
                .andExpect(jsonPath("$[0].criteria[?(@.criterion == 'FLOOR')].verdict").value("MET"))
                .andExpect(jsonPath("$[0].criteria[?(@.criterion == 'PREFERRED_FEATURES')].note")
                        .value("1 z 2: Piwnica"))
                .andExpect(jsonPath("$[1].clientId").value(near))
                .andExpect(jsonPath("$[1].warnings").value(1))
                .andExpect(jsonPath("$[1].criteria[?(@.criterion == 'PRICE')].verdict").value("NEAR"))
                .andExpect(jsonPath("$[1].criteria[?(@.criterion == 'PRICE')].note")
                        .value(hasItem(containsString("6% powyżej"))));
    }

    @Test
    @DisplayName("karta klienta pokazuje pasujące oferty; sprzedana oferta wypada")
    void listsMatchingPropertiesForRequirement() throws Exception {
        String available = createProperty("ACTIVE");
        createProperty("SOLD");

        String clientId = createClient("Kupujący");
        String requirementId = createRequirement(clientId, """
                { "transactionType": "SALE", "propertyTypes": ["APARTMENT"], "locations": [{ "city": "Łódź" }] }
                """);

        mockMvc.perform(get("/api/clients/" + clientId + "/requirements/" + requirementId + "/matches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].property.id").value(available));
    }

    @Test
    @DisplayName("brak danych w ofercie ostrzega zamiast wykluczać; inna dzielnica wyklucza")
    void unknownDataWarnsInsteadOfExcluding() throws Exception {
        String propertyId = createProperty("ACTIVE", "{ \"totalArea\": 47.50, \"roomsCount\": 2 }");

        String noGround = createClient("BezParteru");
        createRequirement(noGround, """
                { "transactionType": "SALE", "propertyTypes": ["APARTMENT"], "floorMin": 1 }
                """);
        createRequirement(createClient("Baluty"), """
                { "transactionType": "SALE", "propertyTypes": ["APARTMENT"],
                  "locations": [{ "city": "Łódź", "district": "Bałuty" }] }
                """);

        mockMvc.perform(get("/api/properties/" + propertyId + "/matches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clientId").value(noGround))
                .andExpect(jsonPath("$[0].warnings").value(1))
                .andExpect(jsonPath("$[0].criteria[?(@.criterion == 'FLOOR')].verdict").value("UNKNOWN"));
    }

    @Test
    @DisplayName("dopasowania innego biura dają 404")
    void isolatesAgencies() throws Exception {
        String propertyId = createProperty("ACTIVE");
        String otherToken = tokenFor("jan@omega.pl", "Omega Nieruchomości");

        mockMvc.perform(get("/api/properties/" + propertyId + "/matches")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }
}
