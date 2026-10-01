package pl.delta.crm.deal;

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
import pl.delta.crm.property.PropertyRepository;
import pl.delta.crm.user.UserRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy tablicy Kanban transakcji.
 *
 * <p>Pokrywają reguły, których nie widać w adnotacjach: jedna otwarta karta na
 * ofertę, obowiązkowy powód przegranej, historia etapów, „najbliższy krok"
 * wzięty z kalendarza oraz izolację między biurami.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfig.class)
class DealModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DealRepository deals;

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

    /** Transakcje trzymają klucze obce do użytkowników. Muszą zniknąć przed nimi. */
    @AfterEach
    void cleanUp() {
        clearAll();
    }

    private void clearAll() {
        events.deleteAll();
        deals.deleteAll();
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

    private String createProperty(String token) throws Exception {
        String body = mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                  "features": ["BALCONY"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("id").asText();
    }

    private String createDeal(String token, String body) throws Exception {
        String response = mockMvc.perform(post("/api/deals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("card").get("id").asText();
    }

    private void createEvent(String token, String dealId, Instant startsAt, String status) throws Exception {
        mockMvc.perform(post("/api/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PRESENTATION",
                                  "status": "%s",
                                  "startsAt": "%s",
                                  "endsAt": "%s",
                                  "dealId": "%s"
                                }
                                """.formatted(status, startsAt, startsAt.plus(1, ChronoUnit.HOURS), dealId)))
                .andExpect(status().isCreated());
    }

    private String createClient(String token, String firstName, String lastName) throws Exception {
        String body = mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "phone": "601 202 303",
                                  "source": "REFERRAL"
                                }
                                """.formatted(firstName, lastName)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("id").asText();
    }

    /** Dodaje zainteresowanego i zwraca jego identyfikator (ostatni na liście). */
    private String addInterest(String token, String dealId, String body) throws Exception {
        String response = mockMvc.perform(post("/api/deals/" + dealId + "/interests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var list = objectMapper.readTree(response).get("interests");
        return list.get(list.size() - 1).get("id").asText();
    }

    // --- testy ---------------------------------------------------------------

    @Test
    @DisplayName("kilku zainteresowanych naraz. Liczniki na karcie i najwyższa oferta")
    void tracksSeveralInterestedBuyers() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String dealId = createDeal(token, "{ \"title\": \"Kawalerka Bałuty\" }");
        String buyerId = createClient(token, "Piotr", "Nowak");

        addInterest(token, dealId, "{ \"clientId\": \"%s\" }".formatted(buyerId));
        addInterest(token, dealId, """
                { "name": "Ewa z ogłoszenia", "phone": "602 303 404", "status": "OFFER", "offerAmount": 512000 }
                """);
        addInterest(token, dealId, "{ \"name\": \"Rezygnujący\", \"status\": \"DROPPED\" }");

        // Ten sam klient drugi raz na liście. Błąd, a nie duplikat.
        mockMvc.perform(post("/api/deals/" + dealId + "/interests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"clientId\": \"%s\" }".formatted(buyerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.clientId").exists());

        // Ani klient, ani imię. Nie wiadomo, kto to.
        mockMvc.perform(post("/api/deals/" + dealId + "/interests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"NEW\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        mockMvc.perform(get("/api/deals")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].interestCount").value(2))
                .andExpect(jsonPath("$[0].offerCount").value(1))
                .andExpect(jsonPath("$[0].bestOffer").value(512000));
    }

    @Test
    @DisplayName("przyjęcie oferty ustawia kupującego i wartość. I może być tylko jedno")
    void acceptingOfferSetsBuyer() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String ownerId = createClient(token, "Marta", "Zielińska");
        String buyerId = createClient(token, "Piotr", "Nowak");
        String dealId = createDeal(token, "{ \"title\": \"Dom w Zgierzu\", \"clientId\": \"%s\" }".formatted(ownerId));

        // Właściciel nie kupuje własnego domu.
        mockMvc.perform(post("/api/deals/" + dealId + "/interests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"clientId\": \"%s\" }".formatted(ownerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.clientId").exists());

        String piotr = addInterest(token, dealId, "{ \"clientId\": \"%s\" }".formatted(buyerId));
        String ewa = addInterest(token, dealId, "{ \"name\": \"Ewa Kowal\", \"status\": \"OFFER\", \"offerAmount\": 700000 }");

        mockMvc.perform(put("/api/deals/" + dealId + "/interests/" + piotr)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"clientId\": \"%s\", \"status\": \"ACCEPTED\", \"offerAmount\": 735000 }"
                                .formatted(buyerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card.buyerId").value(buyerId))
                .andExpect(jsonPath("$.card.buyerName").value("Piotr Nowak"))
                .andExpect(jsonPath("$.card.value").value(735000));

        mockMvc.perform(put("/api/deals/" + dealId + "/interests/" + ewa)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Ewa Kowal\", \"status\": \"ACCEPTED\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists());

        // Cofnięcie przyjęcia zwalnia kupującego.
        mockMvc.perform(put("/api/deals/" + dealId + "/interests/" + piotr)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"clientId\": \"%s\", \"status\": \"CONSIDERING\" }".formatted(buyerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card.buyerId").value(nullValue()))
                .andExpect(jsonPath("$.card.buyerName").value(nullValue()));
    }

    private void move(String token, String dealId, String body) throws Exception {
        mockMvc.perform(put("/api/deals/" + dealId + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    /** Dodaje termin umowny i zwraca identyfikator terminu o podanej dacie. */
    private String addDeadline(String token, String dealId, String type, String date) throws Exception {
        String response = mockMvc.perform(post("/api/deals/" + dealId + "/deadlines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"type\": \"%s\", \"dueDate\": \"%s\" }".formatted(type, date)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        for (var entry : objectMapper.readTree(response).get("deadlines")) {
            if (entry.get("dueDate").asText().equals(date)) {
                return entry.get("id").asText();
            }
        }
        throw new AssertionError("Brak terminu " + date);
    }

    @Test
    @DisplayName("terminy umowne. Najbliższy na karcie, przesunięcie zostawia historię, kalendarz widzi obowiązującą datę")
    void tracksContractDeadlines() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String dealId = createDeal(token, "{ \"title\": \"Kamienica Piotrkowska\", \"stage\": \"MANDATE\" }");

        // Bez rodzaju nie wiadomo, czego pilnować.
        mockMvc.perform(post("/api/deals/" + dealId + "/deadlines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"dueDate\": \"2026-12-31\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.type").exists());

        String mandate = addDeadline(token, dealId, "MANDATE_END", "2026-12-31");
        addDeadline(token, dealId, "EXCLUSIVITY_END", "2026-11-15");

        // Na tablicy: najbliższy otwarty termin.
        mockMvc.perform(get("/api/deals").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].nextDeadline.type").value("EXCLUSIVITY_END"))
                .andExpect(jsonPath("$[0].nextDeadline.dueDate").value("2026-11-15"));

        // Aneks: koniec umowy przesunięty o miesiąc.
        mockMvc.perform(put("/api/deals/" + dealId + "/deadlines/" + mandate + "/move")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"dueDate\": \"2027-01-31\", \"note\": \"Aneks nr 1\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadlines", hasSize(3)))
                .andExpect(jsonPath("$.deadlines[?(@.dueDate == '2026-12-31')].status").value("MOVED"))
                .andExpect(jsonPath("$.deadlines[?(@.dueDate == '2027-01-31')].movedFromDate").value("2026-12-31"));

        // Przesuniętego nie da się już przesunąć ani oznaczyć jako dotrzymany.
        mockMvc.perform(put("/api/deals/" + dealId + "/deadlines/" + mandate + "/met")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        // Kalendarz: listopad–luty, bez przesuniętej daty.
        mockMvc.perform(get("/api/calendar/deadlines")
                        .param("from", "2026-11-01")
                        .param("to", "2027-03-01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].dealId").value(dealId))
                .andExpect(jsonPath("$[1].dueDate").value("2027-01-31"));

        // Dotrzymana wyłączność przestaje być „najbliższym terminem".
        String exclusivity = deadlineIdByDate(token, dealId, "2026-11-15");
        mockMvc.perform(put("/api/deals/" + dealId + "/deadlines/" + exclusivity + "/met")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card.nextDeadline.dueDate").value("2027-01-31"));

        // Inne biuro nie widzi tych terminów w swoim kalendarzu.
        String rival = tokenFor("jan@konkurencja.pl", "Konkurencja");
        mockMvc.perform(get("/api/calendar/deadlines")
                        .param("from", "2026-11-01")
                        .param("to", "2027-03-01")
                        .header("Authorization", "Bearer " + rival))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private String deadlineIdByDate(String token, String dealId, String date) throws Exception {
        String response = mockMvc.perform(get("/api/deals/" + dealId)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        for (var entry : objectMapper.readTree(response).get("deadlines")) {
            if (entry.get("dueDate").asText().equals(date)) {
                return entry.get("id").asText();
            }
        }
        throw new AssertionError("Brak terminu " + date);
    }

    @Test
    @DisplayName("raport lejka liczy najdalszy etap, konwersję, czasy i powody przegranych")
    void reportsFunnel() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        // A: przeskakuje wycenę i wygrywa. W lejku i tak liczy się na „Wycenie".
        String a = createDeal(token, "{ \"title\": \"A\", \"value\": 500000 }");
        move(token, a, "{ \"stage\": \"MANDATE\" }");
        move(token, a, "{ \"stage\": \"WON\" }");
        // B: odpada na wycenie.
        String b = createDeal(token, "{ \"title\": \"B\" }");
        move(token, b, "{ \"stage\": \"VALUATION\" }");
        move(token, b, "{ \"stage\": \"LOST\", \"lostReason\": \"COMPETITOR\" }");
        // C: wciąż otwarty lead.
        createDeal(token, "{ \"title\": \"C\" }");
        // D: założony od razu na wycenie, przegrany na cenie.
        String d = createDeal(token, "{ \"title\": \"D\", \"stage\": \"VALUATION\" }");
        move(token, d, "{ \"stage\": \"LOST\", \"lostReason\": \"PRICE_EXPECTATIONS\" }");

        mockMvc.perform(get("/api/deals/report")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.created").value(4))
                .andExpect(jsonPath("$.totals.open").value(1))
                .andExpect(jsonPath("$.totals.won").value(1))
                .andExpect(jsonPath("$.totals.lost").value(2))
                .andExpect(jsonPath("$.totals.winRate").value(0.3333))
                .andExpect(jsonPath("$.totals.wonValue").value(500000))
                // Szczeble: LEAD, VALUATION, MANDATE, …, WON.
                .andExpect(jsonPath("$.funnel[0].stage").value("LEAD"))
                .andExpect(jsonPath("$.funnel[0].reached").value(4))
                .andExpect(jsonPath("$.funnel[1].reached").value(3))
                .andExpect(jsonPath("$.funnel[1].conversion").value(0.75))
                .andExpect(jsonPath("$.funnel[1].lostHere").value(2))
                .andExpect(jsonPath("$.funnel[2].reached").value(1))
                .andExpect(jsonPath("$.funnel[7].stage").value("WON"))
                .andExpect(jsonPath("$.funnel[7].reached").value(1))
                // Zakończone pobyty: w leadzie A i B, w wycenie B i D.
                .andExpect(jsonPath("$.stageTimes[0].samples").value(2))
                .andExpect(jsonPath("$.stageTimes[1].samples").value(2))
                .andExpect(jsonPath("$.lostReasons", hasSize(2)))
                .andExpect(jsonPath("$.agents", hasSize(1)))
                .andExpect(jsonPath("$.agents[0].won").value(1))
                // Trzy zamknięte transakcje to za mało na szansę wygranej.
                .andExpect(jsonPath("$.forecast.stages[0].winRate").doesNotExist());

        // Okres sprzed założenia transakcji. Pusta kohorta.
        mockMvc.perform(get("/api/deals/report")
                        .param("from", "2020-01-01T00:00:00Z")
                        .param("to", "2020-02-01T00:00:00Z")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.created").value(0))
                .andExpect(jsonPath("$.funnel[1].conversion").doesNotExist());

        // Inne biuro nie widzi nic z tego lejka.
        String rival = tokenFor("jan@konkurencja.pl", "Konkurencja");
        mockMvc.perform(get("/api/deals/report")
                        .header("Authorization", "Bearer " + rival))
                .andExpect(jsonPath("$.totals.created").value(0));
    }

    @Test
    @DisplayName("prognoza waży otwarte karty historyczną szansą wygranej z etapu")
    void forecastsOpenPipeline() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        // Pięć zamkniętych, które doszły do aktywnej sprzedaży: cztery wygrane, jedna przegrana.
        for (int i = 0; i < 5; i++) {
            String id = createDeal(token, "{ \"title\": \"Historia %d\", \"stage\": \"MARKETING\" }".formatted(i));
            move(token, id, i < 4 ? "{ \"stage\": \"WON\" }" : "{ \"stage\": \"LOST\", \"lostReason\": \"OTHER\" }");
        }
        createDeal(token, "{ \"title\": \"Otwarta\", \"stage\": \"MARKETING\", \"value\": 100000 }");

        mockMvc.perform(get("/api/deals/report")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecast.stages[3].stage").value("MARKETING"))
                .andExpect(jsonPath("$.forecast.stages[3].openCount").value(1))
                .andExpect(jsonPath("$.forecast.stages[3].sample").value(5))
                .andExpect(jsonPath("$.forecast.stages[3].winRate").value(0.8))
                .andExpect(jsonPath("$.forecast.stages[3].expectedValue").value(80000))
                .andExpect(jsonPath("$.forecast.expectedValue").value(80000));
    }

    @Test
    @DisplayName("rezultat terminu podpowiada etap karty, ale jej nie przesuwa")
    void completedEventSuggestsStage() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String dealId = createDeal(token, "{ \"title\": \"Kamienica Piotrkowska\", \"stage\": \"MARKETING\" }");

        String response = mockMvc.perform(post("/api/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PRESENTATION",
                                  "startsAt": "2026-11-10T08:00:00Z",
                                  "endsAt": "2026-11-10T09:00:00Z",
                                  "dealId": "%s"
                                }
                                """.formatted(dealId)))
                .andExpect(status().isCreated())
                // Zaplanowany termin niczego nie podpowiada.
                .andExpect(jsonPath("$.suggestion").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String eventId = objectMapper.readTree(response).get("summary").get("id").asText();

        mockMvc.perform(put("/api/calendar/events/" + eventId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"COMPLETED\", \"outcome\": \"OFFER_MADE\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestion.dealId").value(dealId))
                .andExpect(jsonPath("$.suggestion.fromStage").value("MARKETING"))
                .andExpect(jsonPath("$.suggestion.toStage").value("NEGOTIATION"));

        // Karta stoi tam, gdzie stała. Decyzja należy do agenta.
        mockMvc.perform(get("/api/deals/" + dealId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.card.stage").value("MARKETING"));

        // Po przeniesieniu propozycja znika sama.
        mockMvc.perform(put("/api/deals/" + dealId + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"stage\": \"NEGOTIATION\" }"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.suggestion").doesNotExist());
    }

    @Test
    @DisplayName("wspólne oglądanie. Po odbytej prezentacji obaj uczestnicy mają status „oglądał”")
    void jointViewingMarksParticipantsViewed() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String dealId = createDeal(token, "{ \"title\": \"Kamienica Piotrkowska\" }");
        String otherDeal = createDeal(token, "{ \"title\": \"Inna transakcja\" }");
        String first = addInterest(token, dealId, "{ \"name\": \"Jan Wiśniewski\" }");
        String second = addInterest(token, dealId, "{ \"name\": \"Ola Mazur\" }");
        String stranger = addInterest(token, otherDeal, "{ \"name\": \"Obcy\" }");

        String body = """
                {
                  "type": "PRESENTATION",
                  "startsAt": "2026-11-10T08:00:00Z",
                  "endsAt": "2026-11-10T09:00:00Z",
                  "dealId": "%s",
                  "participantIds": [%s]
                }
                """;

        // Uczestnik z innej transakcji nie może trafić na ten termin.
        mockMvc.perform(post("/api/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(dealId, "\"" + stranger + "\"")))
                .andExpect(status().isNotFound());

        String response = mockMvc.perform(post("/api/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(dealId, "\"" + first + "\", \"" + second + "\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.participants", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        String eventId = objectMapper.readTree(response).get("summary").get("id").asText();

        mockMvc.perform(put("/api/calendar/events/" + eventId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"COMPLETED\", \"outcome\": \"INTERESTED\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants[0].status").value("VIEWED"))
                .andExpect(jsonPath("$.participants[1].status").value("VIEWED"));
    }

    @Test
    @DisplayName("karta z ofertą bierze tytuł z adresu, a wartość z ceny oferty")
    void createsDealFromProperty() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createProperty(token);

        mockMvc.perform(post("/api/deals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"propertyId\": \"%s\" }".formatted(propertyId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.card.title").value("Gdańska, Łódź"))
                .andExpect(jsonPath("$.card.stage").value("LEAD"))
                .andExpect(jsonPath("$.card.value").value(549000.00))
                .andExpect(jsonPath("$.card.transactionType").value("SALE"))
                .andExpect(jsonPath("$.history", hasSize(1)))
                .andExpect(jsonPath("$.history[0].fromStage").value(nullValue()));
    }

    @Test
    @DisplayName("oferta może mieć tylko jedną otwartą transakcję")
    void rejectsSecondOpenDealForProperty() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createProperty(token);
        String first = createDeal(token, "{ \"propertyId\": \"%s\" }".formatted(propertyId));

        mockMvc.perform(post("/api/deals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"propertyId\": \"%s\" }".formatted(propertyId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.propertyId").exists());

        // Po przegranej oferta wraca na rynek. Nowa karta jest już dozwolona.
        mockMvc.perform(put("/api/deals/" + first + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"stage\": \"LOST\", \"lostReason\": \"COMPETITOR\" }"))
                .andExpect(status().isOk());

        createDeal(token, "{ \"propertyId\": \"%s\" }".formatted(propertyId));
    }

    @Test
    @DisplayName("przegrana wymaga powodu, a każda zmiana etapu trafia do historii")
    void changesStageWithHistory() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String dealId = createDeal(token, "{ \"title\": \"Telefon w sprawie domu\" }");

        mockMvc.perform(put("/api/deals/" + dealId + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"stage\": \"VALUATION\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card.stage").value("VALUATION"))
                .andExpect(jsonPath("$.card.closedAt").value(nullValue()));

        mockMvc.perform(put("/api/deals/" + dealId + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"stage\": \"LOST\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lostReason").exists());

        mockMvc.perform(put("/api/deals/" + dealId + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"stage\": \"LOST\", \"lostReason\": \"PRICE_EXPECTATIONS\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card.lostReason").value("PRICE_EXPECTATIONS"))
                .andExpect(jsonPath("$.card.closedAt").value(notNullValue()))
                .andExpect(jsonPath("$.history", hasSize(3)))
                .andExpect(jsonPath("$.history[2].fromStage").value("VALUATION"))
                .andExpect(jsonPath("$.history[2].toStage").value("LOST"));

        // Wyciągnięta z „Przegranych" karta gubi stary powód.
        mockMvc.perform(put("/api/deals/" + dealId + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"stage\": \"MANDATE\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card.lostReason").value(nullValue()))
                .andExpect(jsonPath("$.card.closedAt").value(nullValue()));
    }

    @Test
    @DisplayName("tablica pokazuje najbliższy aktywny termin karty")
    void boardShowsNextEvent() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String dealId = createDeal(token, "{ \"title\": \"Kamienica Piotrkowska\" }");
        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

        // Odwołany termin nie jest krokiem naprzód, a późniejszy przegrywa z bliższym.
        createEvent(token, dealId, tomorrow, "CANCELLED");
        createEvent(token, dealId, tomorrow.plus(2, ChronoUnit.DAYS), "PLANNED");
        createEvent(token, dealId, tomorrow.plus(1, ChronoUnit.DAYS), "CONFIRMED");

        mockMvc.perform(get("/api/deals")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nextEvent.status").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].nextEvent.dealId").value(dealId));

        mockMvc.perform(get("/api/deals/" + dealId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events", hasSize(3)));

        // Usunięcie karty zostawia terminy w kalendarzu.
        mockMvc.perform(delete("/api/deals/" + dealId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(events.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("biuro nie widzi transakcji innego biura")
    void isolatesAgencies() throws Exception {
        String delta = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String rival = tokenFor("jan@konkurencja.pl", "Konkurencja");
        String dealId = createDeal(delta, "{ \"title\": \"Dom w Zgierzu\" }");

        mockMvc.perform(get("/api/deals/" + dealId)
                        .header("Authorization", "Bearer " + rival))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/deals")
                        .header("Authorization", "Bearer " + rival))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Nie da się też podpiąć cudzej transakcji pod własny termin.
        mockMvc.perform(post("/api/calendar/events")
                        .header("Authorization", "Bearer " + rival)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "MEETING",
                                  "startsAt": "2026-11-10T08:00:00Z",
                                  "endsAt": "2026-11-10T09:00:00Z",
                                  "dealId": "%s"
                                }
                                """.formatted(dealId)))
                .andExpect(status().isNotFound());
    }
}
