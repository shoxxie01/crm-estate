package pl.delta.crm.search;

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
import pl.delta.crm.client.ClientRepository;
import pl.delta.crm.property.PropertyRepository;
import pl.delta.crm.user.UserRepository;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Szybkie wyszukiwanie z nagłówka. Pilnujemy, że trafia w klientów i oferty,
 * rozumie numer telefonu wpisany z odstępami, nie traktuje {@code %} jako
 * wieloznacznika i nie widzi danych innego biura.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfig.class)
class SearchTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private void createClient(String token, String firstName, String lastName, String phone) throws Exception {
        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "firstName": "%s", "lastName": "%s", "phone": "%s" }
                                """.formatted(firstName, lastName, phone)))
                .andExpect(status().isCreated());
    }

    private void createProperty(String token, String title, String street) throws Exception {
        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "propertyType": "APARTMENT",
                                  "transactionType": "SALE",
                                  "marketType": "SECONDARY",
                                  "status": "ACTIVE",
                                  "title": "%s",
                                  "description": "Mieszkanie po remoncie, druga linia zabudowy.",
                                  "pricing": { "price": 549000.00, "priceCurrency": "PLN" },
                                  "area": { "totalArea": 47.50, "roomsCount": 2, "floorNo": 3, "buildingFloorsCount": 4 },
                                  "address": {
                                    "voivodeship": "LODZKIE",
                                    "county": "Łódź",
                                    "city": "Łódź",
                                    "district": "Polesie",
                                    "street": "%s",
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
                                  "heatingTypes": ["DISTRICT"]
                                }
                                """.formatted(title, street)))
                .andExpect(status().isCreated());
    }

    // --- testy ---------------------------------------------------------------

    @Test
    @DisplayName("Fraza trafia w klientów po nazwisku i w oferty po ulicy")
    void findsClientsAndProperties() throws Exception {
        createClient(token, "Piotr", "Gdański", "605 405 932");
        createClient(token, "Ewa", "Zielińska", "501 222 333");
        createProperty(token, "Dwupokojowe z balkonem", "Gdańska");
        createProperty(token, "Kawalerka przy parku", "Piotrkowska");

        mockMvc.perform(get("/api/search").param("q", "GDAŃS")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients", hasSize(1)))
                .andExpect(jsonPath("$.clients[0].lastName").value("Gdański"))
                .andExpect(jsonPath("$.clientsTotal").value(1))
                .andExpect(jsonPath("$.properties", hasSize(1)))
                .andExpect(jsonPath("$.properties[0].street").value("Gdańska"))
                .andExpect(jsonPath("$.properties[0].city").value("Łódź"))
                .andExpect(jsonPath("$.propertiesTotal").value(1));
    }

    @Test
    @DisplayName("Numer telefonu wpisany z odstępami znajduje klienta")
    void findsClientByFormattedPhone() throws Exception {
        createClient(token, "Piotr", "Nowak", "605405932");

        mockMvc.perform(get("/api/search").param("q", "405 932")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients", hasSize(1)))
                .andExpect(jsonPath("$.clients[0].lastName").value("Nowak"));
    }

    @Test
    @DisplayName("Za krótka fraza i znak % nie zwracają całej bazy")
    void shortOrWildcardQueryReturnsNothing() throws Exception {
        createClient(token, "Piotr", "Nowak", "605405932");

        mockMvc.perform(get("/api/search").param("q", "n")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients", hasSize(0)));

        mockMvc.perform(get("/api/search").param("q", "%%")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients", hasSize(0)))
                .andExpect(jsonPath("$.properties", hasSize(0)));
    }

    @Test
    @DisplayName("Dane innego biura są niewidoczne")
    void otherAgencyIsInvisible() throws Exception {
        String other = tokenFor("jan@inne.pl", "Inne Biuro");
        createClient(other, "Piotr", "Nowak", "605405932");
        createProperty(other, "Dom z ogrodem", "Nowakowskiego");

        mockMvc.perform(get("/api/search").param("q", "nowak")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients", hasSize(0)))
                .andExpect(jsonPath("$.properties", hasSize(0)));
    }

    @Test
    @DisplayName("Bez tokenu brak dostępu")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "nowak"))
                .andExpect(status().isUnauthorized());
    }
}
