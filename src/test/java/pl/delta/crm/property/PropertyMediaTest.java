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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.delta.crm.PostgresTestcontainerConfig;
import pl.delta.crm.user.UserRepository;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy galerii oferty na prawdziwym MinIO.
 *
 * <p>Atrapa storage'u nie miałaby tu czego sprawdzić: cała wartość tych testów
 * siedzi w tym, że plik faktycznie ląduje w buckecie, że kasowanie go stamtąd
 * usuwa, a podpisany link daje się wygenerować. Kontener jest statyczny i
 * konfigurowany przez {@code @DynamicPropertySource}, bo Spring Boot nie ma dla
 * MinIO adnotacji {@code @ServiceConnection} — w przeciwieństwie do Postgresa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(PostgresTestcontainerConfig.class)
class PropertyMediaTest {

    @Container
    static final MinIOContainer MINIO = new MinIOContainer("minio/minio:latest");

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("delta.storage.endpoint", MINIO::getS3URL);
        registry.add("delta.storage.access-key", MINIO::getUserName);
        registry.add("delta.storage.secret-key", MINIO::getPassword);
        registry.add("delta.storage.bucket", () -> "test-media");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PropertyRepository properties;

    @Autowired
    private PropertyMediaRepository media;

    @Autowired
    private UserRepository users;

    @Autowired
    private S3Client s3;

    @BeforeEach
    void clearData() {
        properties.deleteAll();
        users.deleteAll();
    }

    @AfterEach
    void clearProperties() {
        properties.deleteAll();
    }

    // --- testy ---------------------------------------------------------------

    @Test
    @DisplayName("wgranie dwóch zdjęć nadaje pozycje 0 i 1 oraz zwraca podpisane linki")
    void uploadsPhotos() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createFlat(token);

        mockMvc.perform(multipart("/api/properties/{id}/media", propertyId)
                        .file(jpeg("files", "salon.jpg", 1200, 900))
                        .file(jpeg("files", "kuchnia.jpg", 1200, 900))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[0].fileName").value("salon.jpg"))
                .andExpect(jsonPath("$[0].mediaType").value("PHOTO"))
                .andExpect(jsonPath("$[0].url").isNotEmpty())
                .andExpect(jsonPath("$[0].thumbnailUrl").isNotEmpty())
                .andExpect(jsonPath("$[0].meetsPortalRequirements").value(true))
                .andExpect(jsonPath("$[1].position").value(1))
                // Front nie ma prawa poznać układu bucketa.
                .andExpect(jsonPath("$[0].storageKey").doesNotExist());
    }

    @Test
    @DisplayName("zdjęcie większe niż 1920 px jest skalowane przy wgrywaniu")
    void downscalesLargePhotos() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createFlat(token);

        mockMvc.perform(multipart("/api/properties/{id}/media", propertyId)
                        .file(jpeg("files", "duze.jpg", 4000, 3000))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].widthPx").value(1920))
                .andExpect(jsonPath("$[0].heightPx").value(1440))
                // Po normalizacji plik mieści się w limicie portalu (5 MB).
                .andExpect(jsonPath("$[0].meetsPortalRequirements").value(true));
    }

    @Test
    @DisplayName("dopiero zdjęcie domyka gotowość oferty do eksportu")
    void photoCompletesExportReadiness() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createFlat(token);

        mockMvc.perform(get("/api/properties/{id}", propertyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.readyForExport").value(false));

        mockMvc.perform(multipart("/api/properties/{id}/media", propertyId)
                        .file(jpeg("files", "salon.jpg", 1200, 900))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/properties/{id}", propertyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.readyForExport").value(true))
                .andExpect(jsonPath("$.media.length()").value(1));
    }

    @Test
    @DisplayName("podmiana pliku zachowuje pozycję, podpis i identyfikator zdjęcia")
    void replacingFileKeepsPositionAndCaption() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createFlat(token);

        List<String> ids = upload(token, propertyId, "pierwsze.jpg", "drugie.jpg");
        String second = ids.get(1);

        mockMvc.perform(patch("/api/properties/{p}/media/{m}", propertyId, second)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caption\":\"Widok z balkonu\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/api/properties/{p}/media/{m}/file", propertyId, second)
                        .file(jpeg("file", "poprawione.jpg", 1600, 1200))
                        .header("Authorization", "Bearer " + token)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(second))
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.caption").value("Widok z balkonu"))
                .andExpect(jsonPath("$.fileName").value("poprawione.jpg"))
                .andExpect(jsonPath("$.widthPx").value(1600));
    }

    @Test
    @DisplayName("usunięcie zdjęcia przenumerowuje resztę i kasuje plik ze storage'u")
    void deletingPhotoRenumbersAndRemovesObject() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createFlat(token);

        List<String> ids = upload(token, propertyId, "a.jpg", "b.jpg", "c.jpg");
        String storageKey = storageKeyOf(propertyId, ids.getFirst());

        mockMvc.perform(delete("/api/properties/{p}/media/{m}", propertyId, ids.getFirst())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/properties/{id}/media", propertyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(2))
                // Pozycje muszą zostać ciągiem od zera — inaczej „zdjęcie główne"
                // przestaje być pierwsze.
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[0].fileName").value("b.jpg"))
                .andExpect(jsonPath("$[1].position").value(1));

        assertThatThrownBy(() -> s3.headObject(HeadObjectRequest.builder()
                .bucket("test-media").key(storageKey).build()))
                .isInstanceOf(NoSuchKeyException.class);
    }

    @Test
    @DisplayName("zmiana kolejności przestawia zdjęcie główne")
    void reorderChangesCoverPhoto() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createFlat(token);

        List<String> ids = upload(token, propertyId, "a.jpg", "b.jpg", "c.jpg");
        String body = objectMapper.writeValueAsString(
                Map.of("mediaIds",List.of(ids.get(2), ids.get(0), ids.get(1))));

        mockMvc.perform(put("/api/properties/{id}/media/order", propertyId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("c.jpg"))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[2].fileName").value("b.jpg"))
                .andExpect(jsonPath("$[2].position").value(2));
    }

    @Test
    @DisplayName("niepełna lista przy zmianie kolejności jest odrzucana")
    void rejectsPartialReorder() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createFlat(token);

        List<String> ids = upload(token, propertyId, "a.jpg", "b.jpg");
        String body = objectMapper.writeValueAsString(
                Map.of("mediaIds",List.of(ids.getFirst())));

        mockMvc.perform(put("/api/properties/{id}/media/order", propertyId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.mediaIds").isNotEmpty());
    }

    @Test
    @DisplayName("plik, który nie jest obrazem, jest odrzucany mimo poprawnego Content-Type")
    void rejectsNonImage() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createFlat(token);

        // Nagłówek mówi, że to JPEG — o formacie decyduje zawartość, nie deklaracja.
        MockMultipartFile fake = new MockMultipartFile("files", "wirus.jpg", "image/jpeg",
                "MZ to nie jest obrazek".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/properties/{id}/media", propertyId)
                        .file(fake)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.files").isNotEmpty());
    }

    @Test
    @DisplayName("zdjęcie poniżej 400×300 px jest odrzucane — portal go nie przyjmie")
    void rejectsTooSmallPhoto() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createFlat(token);

        mockMvc.perform(multipart("/api/properties/{id}/media", propertyId)
                        .file(jpeg("files", "male.jpg", 320, 240))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.files").isNotEmpty());
    }

    @Test
    @DisplayName("obce biuro nie doda zdjęcia do cudzej oferty — 404, nie 403")
    void isolatesAgencies() throws Exception {
        String ownerToken = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createFlat(ownerToken);
        String otherToken = tokenFor("marek@omega.pl", "Omega Nieruchomości");

        mockMvc.perform(multipart("/api/properties/{id}/media", propertyId)
                        .file(jpeg("files", "podmiana.jpg", 800, 600))
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/properties/{id}/media", propertyId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("miniatura zdjęcia głównego wraca w wierszu listy ofert")
    void listExposesCoverThumbnail() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createFlat(token);

        mockMvc.perform(get("/api/properties")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content[0].coverThumbnailUrl").doesNotExist());

        upload(token, propertyId, "salon.jpg");

        mockMvc.perform(get("/api/properties")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content[0].coverThumbnailUrl").isNotEmpty());
    }

    @Test
    @DisplayName("powyżej 50 materiałów oferta nie przyjmuje kolejnych")
    void enforcesLimitPerProperty() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createFlat(token);

        var request = multipart("/api/properties/{id}/media", propertyId)
                .header("Authorization", "Bearer " + token);
        for (int i = 0; i <= PropertyMedia.MAX_PER_PROPERTY; i++) {
            request = request.file(jpeg("files", "zdjecie-" + i + ".jpg", 600, 450));
        }

        // Limit sprawdzany przed zapisem, więc odrzucona paczka nie zostawia
        // w storage nawet pierwszego pliku.
        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.files").isNotEmpty());

        mockMvc.perform(get("/api/properties/{id}/media", propertyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("skasowanie całej oferty zabiera też jej pliki ze storage'u")
    void deletingPropertyPurgesStorage() throws Exception {
        String token = tokenFor("anna@delta.pl", "Delta Nieruchomości");
        String propertyId = createFlat(token);

        List<String> ids = upload(token, propertyId, "a.jpg", "b.jpg");
        List<String> keys = ids.stream().map(id -> storageKeyOf(propertyId, id)).toList();

        mockMvc.perform(delete("/api/properties/{id}", propertyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Wiersze znikają kaskadą bazy, ale kaskada nie wie nic o buckecie —
        // bez jawnego sprzątania zostawałby tu komplet zdjęć skasowanej oferty.
        for (String key : keys) {
            assertThatThrownBy(() -> s3.headObject(HeadObjectRequest.builder()
                    .bucket("test-media").key(key).build()))
                    .isInstanceOf(NoSuchKeyException.class);
        }
    }

    // --- narzędzia -----------------------------------------------------------

    /**
     * JPEG generowany w locie zamiast pliku w repozytorium — test ma sam
     * decydować o wymiarach, a binaria w repo i tak nikt nie przegląda.
     */
    private static MockMultipartFile jpeg(String part, String name, int width, int height)
            throws IOException {

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(120, 140, 180));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return new MockMultipartFile(part, name, "image/jpeg", out.toByteArray());
    }

    private List<String> upload(String token, String propertyId, String... names) throws Exception {
        var request = multipart("/api/properties/{id}/media", propertyId)
                .header("Authorization", "Bearer " + token);
        for (String name : names) {
            request = request.file(jpeg("files", name, 800, 600));
        }

        String body = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        List<String> ids = new ArrayList<>();
        for (JsonNode node : objectMapper.readTree(body)) {
            ids.add(node.get("id").asText());
        }
        return ids;
    }

    /** Klucz w storage nie wychodzi przez API, więc do asercji bierzemy go z bazy. */
    private String storageKeyOf(String propertyId, String mediaId) {
        return media.findByPropertyIdOrderByPositionAsc(UUID.fromString(propertyId)).stream()
                .filter(item -> item.getId().toString().equals(mediaId))
                .findFirst()
                .orElseThrow()
                .getStorageKey();
    }

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

    private String createFlat(String token) throws Exception {
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
                                  "area": { "totalArea": 47.50, "roomsCount": 2 },
                                  "address": {
                                    "voivodeship": "LODZKIE",
                                    "county": "Łódź",
                                    "city": "Łódź",
                                    "street": "Gdańska",
                                    "postalCode": "90-001"
                                  },
                                  "energy": { "energyPrimary": 132.40, "energyClass": "D" }
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(body).get("readyForExport").asBoolean()).isFalse();
        return objectMapper.readTree(body).get("id").asText();
    }
}
