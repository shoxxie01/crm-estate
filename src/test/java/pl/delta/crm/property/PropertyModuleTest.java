package pl.delta.crm.property;

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
import pl.delta.crm.user.UserRepository;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy modułu nieruchomości.
 *
 * <p>Poza samą ścieżką dodawania oferty pokrywają dwie rzeczy, które łatwo
 * zepsuć bezgłośnie: izolację danych między biurami oraz walidację pól, bez
 * których portal ogłoszeniowy odrzuci ofertę. Przy okazji migracje przechodzą
 * przez prawdziwego Postgresa, więc weryfikowany jest także schemat.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfig.class)
class PropertyModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PropertyRepository properties;

    @Autowired
    private UserRepository users;

    @BeforeEach
    void clearData() {
        properties.deleteAll();
        users.deleteAll();
    }

    /**
     * Oferty trzymają klucz obcy do użytkownika, więc muszą zniknąć również po
     * tej klasie — inaczej {@code users.deleteAll()} w innym teście przewróci się
     * na więzach, zależnie od kolejności uruchomienia.
     */
    @AfterEach
    void clearProperties() {
        properties.deleteAll();
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

    /** Mieszkanie z kompletem pól wymaganych przez portale. */
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
                  "features": ["BALCONY", "BASEMENT", "INTERCOM", "INTERNET"]
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

    private String createFlat(String token) throws Exception {
        String body = mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(flatBody()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("id").asText();
    }

    // --- testy ---------------------------------------------------------------

    @Test
    @DisplayName("dodanie oferty zwraca 201 i zapisuje komplet pól")
    void createsProperty() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(flatBody()))
                .andExpect(status().isCreated())
                // Numer nadaje serwis; wzorzec zamiast konkretnej wartości,
                // żeby test nie zaczął zależeć od bieżącego miesiąca.
                .andExpect(jsonPath("$.referenceNumber").value(matchesPattern("\\d{4}/\\d{2}/001")))
                .andExpect(jsonPath("$.propertyType").value("APARTMENT"))
                .andExpect(jsonPath("$.marketType").value("SECONDARY"))
                .andExpect(jsonPath("$.pricing.price").value(549000.00))
                .andExpect(jsonPath("$.pricing.pricePerSquareMeter").value(11557.89))
                .andExpect(jsonPath("$.address.city").value("Łódź"))
                // domyślnie nie publikujemy dokładnego adresu
                .andExpect(jsonPath("$.address.hideExactAddress").value(true))
                .andExpect(jsonPath("$.building.buildYear").value(1938))
                .andExpect(jsonPath("$.energy.satisfiesLegalRequirement").value(true))
                .andExpect(jsonPath("$.features").isArray())
                .andExpect(jsonPath("$.agent.email").value("anna@delta.pl"));
    }

    @Test
    @DisplayName("oferta bez zdjęć nie jest gotowa do eksportu")
    void notReadyForExportWithoutPhotos() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(flatBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.readyForExport").value(false));
    }

    @Test
    @DisplayName("mieszkanie bez liczby pokoi jest odrzucane — portal wymaga RoomsNum")
    void rejectsFlatWithoutRoomsCount() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        String body = flatBody().replace("\"roomsCount\": 2,", "");

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['area.roomsCount']").isNotEmpty());
    }

    @Test
    @DisplayName("tytuł dłuższy niż 50 znaków jest odrzucany — taki limit ma Otodom")
    void rejectsTooLongTitle() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        String body = flatBody().replace(
                "Dwupokojowe z balkonem, Stare Polesie",
                "Przestronne dwupokojowe mieszkanie z dużym balkonem w samym sercu Starego Polesia");

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").isNotEmpty());
    }

    @Test
    @DisplayName("zwolnienie ze świadectwa energetycznego wymaga uzasadnienia")
    void rejectsEnergyExemptionWithoutReason() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        String body = flatBody().replace(
                "\"energy\": { \"energyPrimary\": 132.40, \"energyClass\": \"D\" }",
                "\"energy\": { \"exempt\": true }");

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['energy.exemptNote']").isNotEmpty());
    }

    @Test
    @DisplayName("kolejne oferty dostają rosnące numery, bez udziału użytkownika")
    void assignsSequentialReferenceNumbers() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        for (String expected : new String[]{"001", "002", "003"}) {
            mockMvc.perform(post("/api/properties")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(flatBody()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.referenceNumber")
                            .value(matchesPattern("\\d{4}/\\d{2}/" + expected)));
        }
    }

    @Test
    @DisplayName("numeracja biegnie osobno w każdym biurze")
    void numbersRunPerAgency() throws Exception {
        String delta = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String omega = tokenFor("piotr@omega.pl", "Omega Nieruchomości");

        createFlat(delta);

        // Drugie biuro zaczyna od 001, mimo że taki numer istnieje już u pierwszego.
        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + omega)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(flatBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenceNumber").value(matchesPattern("\\d{4}/\\d{2}/001")));
    }

    @Test
    @DisplayName("oferta bez powiatu zapisuje się, ale nie jest gotowa do eksportu")
    void allowsMissingCountyButBlocksExport() throws Exception {
        // Powiat, gmina i dzielnica są opcjonalne przy zapisie — portal wymaga
        // powiatu, więc brak wychodzi dopiero na gotowości do eksportu.
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        String body = flatBody()
                .replace("\"county\": \"Łódź\",", "")
                .replace("\"district\": \"Polesie\",", "");

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.address.county").doesNotExist())
                .andExpect(jsonPath("$.readyForExport").value(false));
    }

    @Test
    @DisplayName("oferta jednego biura jest niewidoczna dla drugiego")
    void isolatesPropertiesBetweenAgencies() throws Exception {
        String delta = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String omega = tokenFor("piotr@omega.pl", "Omega Nieruchomości");

        String propertyId = createFlat(delta);

        // właściciel widzi ofertę
        mockMvc.perform(get("/api/properties/" + propertyId)
                        .header("Authorization", "Bearer " + delta))
                .andExpect(status().isOk());

        // obce biuro dostaje 404, nie 403 — inaczej dałoby się sprawdzać istnienie ofert
        mockMvc.perform(get("/api/properties/" + propertyId)
                        .header("Authorization", "Bearer " + omega))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/properties")
                        .header("Authorization", "Bearer " + omega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/properties")
                        .header("Authorization", "Bearer " + delta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("bez tokenu API nieruchomości jest zamknięte")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("słowniki zwracają cechy pogrupowane kategoriami")
    void servesDictionaries() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        mockMvc.perform(get("/api/properties/dictionaries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyType[0].value").value("APARTMENT"))
                .andExpect(jsonPath("$.propertyType[0].label").value("Mieszkanie"))
                .andExpect(jsonPath("$.voivodeship.length()").value(16))
                .andExpect(jsonPath("$.featureGroups[0].category").value("ADDITIONAL"))
                .andExpect(jsonPath("$.featureGroups[0].features").isArray());
    }

    @Test
    @DisplayName("klasa energetyczna A+ zapisuje się poprawnie")
    void acceptsEnergyClassAPlus() throws Exception {
        // Do bazy trafia nazwa stałej (`A_PLUS`), nie etykieta („A+") — kolumna
        // musi być na tyle szeroka. Wcześniej miała VARCHAR(3) i insert padał.
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        String body = flatBody().replace("\"energyClass\": \"D\"", "\"energyClass\": \"A_PLUS\"");

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.energy.energyClass").value("A_PLUS"));
    }

    @Test
    @DisplayName("wartość spoza słownika daje 400 z komunikatem, a nie 401")
    void rejectsUnknownDictionaryValueWithBadRequest() throws Exception {
        // 401 w tym miejscu wyglądałby dla klienta jak wygasła sesja i wylogowywał
        // użytkownika w środku wypełniania formularza.
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        String body = flatBody()
                .replace("\"buildingType\": \"TENEMENT\"", "\"buildingType\": \"WIEZOWIEC\"");

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    @DisplayName("pokoju nie da się wystawić na sprzedaż")
    void rejectsRoomForSale() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");

        String body = flatBody()
                .replace("\"propertyType\": \"APARTMENT\"", "\"propertyType\": \"ROOM\"");

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.transactionType").isNotEmpty());
    }
}
