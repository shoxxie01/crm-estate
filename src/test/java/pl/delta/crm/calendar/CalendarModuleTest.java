package pl.delta.crm.calendar;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy modułu kalendarza.
 *
 * <p>Pokrywają to, co w tym module najłatwiej zepsuć bezgłośnie: powiązanie
 * terminu z ofertą i klientem, zapytanie o zakres dat (przecięcie, nie
 * zawieranie), izolację między biurami oraz dwie reguły, których nie widać
 * w adnotacjach. Rezultat wyłącznie dla odbytego terminu i ostrzeżenie
 * o kolizji, które ma <b>nie</b> blokować zapisu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfig.class)
class CalendarModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    /**
     * Terminy trzymają klucze obce do ofert, klientów i użytkowników, więc muszą
     * zniknąć również po tej klasie. Inaczej {@code users.deleteAll()} w innym
     * teście przewróci się na więzach, zależnie od kolejności uruchomienia.
     */
    @AfterEach
    void cleanUp() {
        clearAll();
    }

    private void clearAll() {
        events.deleteAll();
        properties.deleteAll();
        clients.deleteAll();
        users.deleteAll();
    }

    // --- dane testowe --------------------------------------------------------

    private static String registerBody(String email, String agency) {
        return """
                {
                  "firstName": "Anna",
                  "lastName": "Kowalska",
                  "email": "%s",
                  "password": "TajneHaslo123!",
                  "agencyName": "%s"
                }
                """.formatted(email, agency);
    }

    private static String flatBody() {
        return """
                {
                  "propertyType": "APARTMENT",
                  "transactionType": "SALE",
                  "marketType": "SECONDARY",
                  "status": "ACTIVE",
                  "title": "Dwupokojowe z balkonem, Stare Polesie",
                  "description": "Mieszkanie po remoncie, druga linia zabudowy, cicho.",
                  "pricing": { "price": 549000.00, "priceCurrency": "PLN" },
                  "area": { "totalArea": 47.50, "roomsCount": 2, "floorNo": 3, "buildingFloorsCount": 4 },
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
                """;
    }

    private static String clientBody() {
        return """
                {
                  "firstName": "Marta",
                  "lastName": "Zielińska",
                  "phone": "+048 601 202 303",
                  "source": "REFERRAL"
                }
                """;
    }

    private String tokenFor(String email, String agency) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, agency)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("token").asText();
    }

    private String createProperty(String token) throws Exception {
        String body = mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(flatBody()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("id").asText();
    }

    private String createClient(String token) throws Exception {
        String body = mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientBody()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("id").asText();
    }

    private static String eventBody(String type, String startsAt, String endsAt) {
        return """
                {
                  "type": "%s",
                  "startsAt": "%s",
                  "endsAt": "%s"
                }
                """.formatted(type, startsAt, endsAt);
    }

    private String createEvent(String token, String body) throws Exception {
        String response = mockMvc.perform(post("/api/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("summary").get("id").asText();
    }

    // --- testy ---------------------------------------------------------------

    @Test
    @DisplayName("termin można powiązać z ofertą i z klientem naraz")
    void createsEventLinkedToPropertyAndClient() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createProperty(token);
        String clientId = createClient(token);

        String body = """
                {
                  "type": "CONTRACT_SIGNING",
                  "startsAt": "2026-09-10T08:00:00Z",
                  "endsAt": "2026-09-10T09:00:00Z",
                  "propertyId": "%s",
                  "clientId": "%s"
                }
                """.formatted(propertyId, clientId);

        mockMvc.perform(post("/api/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary.propertyId").value(propertyId))
                .andExpect(jsonPath("$.summary.clientId").value(clientId))
                .andExpect(jsonPath("$.summary.clientName").value("Marta Zielińska"))
                .andExpect(jsonPath("$.summary.status").value("PLANNED"))
                // Miejsce bierze się z adresu oferty, bez przepisywania go ręcznie.
                .andExpect(jsonPath("$.summary.location").value("Gdańska, Łódź"));
    }

    @Test
    @DisplayName("pusty tytuł jest składany z rodzaju i adresu oferty")
    void generatesTitleFromTypeAndProperty() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createProperty(token);

        String body = """
                {
                  "type": "PRESENTATION",
                  "startsAt": "2026-09-11T10:00:00Z",
                  "endsAt": "2026-09-11T11:00:00Z",
                  "propertyId": "%s",
                  "counterpartyName": "Piotr Nowak"
                }
                """.formatted(propertyId);

        mockMvc.perform(post("/api/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary.title").value("Prezentacja. Gdańska, Łódź"))
                .andExpect(jsonPath("$.summary.counterpartyName").value("Piotr Nowak"));
    }

    @Test
    @DisplayName("lista zwraca terminy przecinające zakres, nie tylko zaczynające się w nim")
    void listsEventsOverlappingRange() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        // Termin trwający od poniedziałku do środy. Pytamy o sam wtorek.
        createEvent(token, eventBody("OPEN_HOUSE", "2026-09-07T06:00:00Z", "2026-09-09T18:00:00Z"));

        mockMvc.perform(get("/api/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-09-08T00:00:00Z")
                        .param("to", "2026-09-09T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Tydzień później nie ma już nic.
        mockMvc.perform(get("/api/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-09-15T00:00:00Z")
                        .param("to", "2026-09-16T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("kolidujący termin zapisuje się, ale wraca z ostrzeżeniem")
    void reportsConflictsWithoutBlockingSave() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        createEvent(token, eventBody("PRESENTATION", "2026-09-10T10:00:00Z", "2026-09-10T11:00:00Z"));

        mockMvc.perform(post("/api/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody("VALUATION", "2026-09-10T10:30:00Z", "2026-09-10T11:30:00Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conflicts.length()").value(1));

        // Termin stykający się krańcem nie jest kolizją.
        mockMvc.perform(post("/api/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody("MEETING", "2026-09-10T11:30:00Z", "2026-09-10T12:00:00Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conflicts.length()").value(0));
    }

    @Test
    @DisplayName("koniec terminu musi być po jego początku")
    void rejectsInvertedRange() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        mockMvc.perform(post("/api/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody("MEETING", "2026-09-10T12:00:00Z", "2026-09-10T11:00:00Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.endsAt").isNotEmpty());
    }

    @Test
    @DisplayName("rezultat bez statusu „odbyło się” jest odrzucany")
    void rejectsOutcomeWithoutHappenedStatus() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        String body = """
                {
                  "type": "PRESENTATION",
                  "status": "PLANNED",
                  "startsAt": "2026-09-10T10:00:00Z",
                  "endsAt": "2026-09-10T11:00:00Z",
                  "outcome": "INTERESTED"
                }
                """;

        mockMvc.perform(post("/api/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.outcome").isNotEmpty());
    }

    @Test
    @DisplayName("domknięcie terminu zapisuje rezultat, a cofnięcie statusu go czyści")
    void closesEventWithOutcome() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String eventId = createEvent(token,
                eventBody("PRESENTATION", "2026-09-10T10:00:00Z", "2026-09-10T11:00:00Z"));

        mockMvc.perform(put("/api/calendar/events/" + eventId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED",
                                  "outcome": "PRICE_TOO_HIGH",
                                  "outcomeNote": "Klient wraca, jeśli właściciel zejdzie o 30 tys."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.outcome").value("PRICE_TOO_HIGH"))
                .andExpect(jsonPath("$.outcomeNote").isNotEmpty());

        mockMvc.perform(put("/api/calendar/events/" + eventId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"CANCELLED\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.outcome").doesNotExist())
                .andExpect(jsonPath("$.outcomeNote").doesNotExist());
    }

    @Test
    @DisplayName("terminy innego biura są niewidoczne i dają 404")
    void isolatesAgencies() throws Exception {
        String ourToken = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String eventId = createEvent(ourToken,
                eventBody("PRESENTATION", "2026-09-10T10:00:00Z", "2026-09-10T11:00:00Z"));

        String otherToken = tokenFor("jan@omega.pl", "Omega Nieruchomości");

        mockMvc.perform(get("/api/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/calendar/events")
                        .header("Authorization", "Bearer " + otherToken)
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-10-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("karta oferty i karta klienta dostają swoje terminy")
    void listsEventsForPropertyAndClient() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createProperty(token);
        String clientId = createClient(token);

        createEvent(token, """
                {
                  "type": "PRESENTATION",
                  "startsAt": "2026-09-10T10:00:00Z",
                  "endsAt": "2026-09-10T11:00:00Z",
                  "propertyId": "%s",
                  "clientId": "%s"
                }
                """.formatted(propertyId, clientId));

        mockMvc.perform(get("/api/properties/" + propertyId + "/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/clients/" + clientId + "/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("skasowanie oferty odpina jej terminy zamiast blokować usunięcie")
    void deletingPropertyDetachesItsEvents() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createProperty(token);

        String eventId = createEvent(token, """
                {
                  "type": "PRESENTATION",
                  "title": "Prezentacja. Gdańska 41",
                  "startsAt": "2026-09-10T10:00:00Z",
                  "endsAt": "2026-09-10T11:00:00Z",
                  "propertyId": "%s"
                }
                """.formatted(propertyId));

        // Klucz obcy calendar_events.property_id nie ma ON DELETE, więc bez
        // jawnego odpięcia baza odrzucała kasowanie. A termin do oferty umawia
        // się przy każdej normalnej ofercie.
        mockMvc.perform(delete("/api/properties/" + propertyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Historia zostaje: sam termin dalej istnieje, traci tylko powiązanie.
        mockMvc.perform(get("/api/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.propertyId").doesNotExist())
                .andExpect(jsonPath("$.summary.title").value("Prezentacja. Gdańska 41"));
    }

    @Test
    @DisplayName("skasowanie klienta odpina jego terminy zamiast blokować usunięcie")
    void deletingClientDetachesItsEvents() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String clientId = createClient(token);

        String eventId = createEvent(token, """
                {
                  "type": "MEETING",
                  "title": "Spotkanie z właścicielem",
                  "startsAt": "2026-09-11T10:00:00Z",
                  "endsAt": "2026-09-11T11:00:00Z",
                  "clientId": "%s"
                }
                """.formatted(clientId));

        mockMvc.perform(delete("/api/clients/" + clientId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.clientId").doesNotExist())
                .andExpect(jsonPath("$.summary.title").value("Spotkanie z właścicielem"));
    }
}
