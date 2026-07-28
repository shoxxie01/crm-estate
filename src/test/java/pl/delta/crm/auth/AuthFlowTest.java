package pl.delta.crm.auth;

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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfig.class)
class AuthFlowTest {

    private static final String REGISTER_BODY = """
            {
              "firstName": "Anna",
              "lastName": "Kowalska",
              "email": "anna.kowalska@delta.pl",
              "password": "TajneHaslo123!",
              "agencyName": "Delta Nieruchomości"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository users;

    @BeforeEach
    void clearUsers() {
        users.deleteAll();
    }

    private String registerAndGetToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("token").asText();
    }

    @Test
    @DisplayName("rejestracja zwraca token i użytkownika bez hasła")
    void registerReturnsToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("anna.kowalska@delta.pl"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();

        JsonNode user = objectMapper.readTree(body).get("user");
        assertThat(user.has("passwordHash")).isFalse();
        assertThat(user.has("password")).isFalse();
    }

    @Test
    @DisplayName("hasło jest zapisywane jako hash, nie plaintext")
    void passwordIsHashed() throws Exception {
        registerAndGetToken();

        String storedHash = users.findByEmail("anna.kowalska@delta.pl").orElseThrow().getPasswordHash();
        assertThat(storedHash).isNotEqualTo("TajneHaslo123!").startsWith("$2");
    }

    @Test
    @DisplayName("drugie konto na ten sam e-mail zwraca 409 z błędem pola")
    void duplicateEmailIsRejected() throws Exception {
        registerAndGetToken();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Konto z tym adresem już istnieje."))
                .andExpect(jsonPath("$.errors.email").isNotEmpty());
    }

    @Test
    @DisplayName("za krótkie hasło zwraca 400 z mapą błędów pól")
    void shortPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY.replace("TajneHaslo123!", "krotkie")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Hasło musi mieć co najmniej 8 znaków."));
    }

    @Test
    @DisplayName("logowanie poprawnym hasłem zwraca token")
    void loginSucceeds() throws Exception {
        registerAndGetToken();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "anna.kowalska@delta.pl", "password": "TajneHaslo123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.firstName").value("Anna"));
    }

    @Test
    @DisplayName("e-mail przy logowaniu jest niewrażliwy na wielkość liter")
    void loginIsCaseInsensitive() throws Exception {
        registerAndGetToken();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "Anna.Kowalska@Delta.pl", "password": "TajneHaslo123!"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("złe hasło zwraca 401 bez zdradzania, czy konto istnieje")
    void wrongPasswordIsRejected() throws Exception {
        registerAndGetToken();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "anna.kowalska@delta.pl", "password": "ZupelnieInne1!"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Nieprawidłowy e-mail lub hasło."));
    }

    @Test
    @DisplayName("nieznany e-mail daje ten sam komunikat co złe hasło")
    void unknownEmailGivesSameMessage() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nikt@delta.pl", "password": "TajneHaslo123!"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Nieprawidłowy e-mail lub hasło."));
    }

    @Test
    @DisplayName("/me bez tokenu zwraca 401")
    void meRequiresToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    @DisplayName("/me z tokenem zwraca zalogowanego użytkownika")
    void meReturnsCurrentUser() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("anna.kowalska@delta.pl"))
                .andExpect(jsonPath("$.agencyName").value("Delta Nieruchomości"));
    }

    @Test
    @DisplayName("podrobiony token jest odrzucany")
    void tamperedTokenIsRejected() throws Exception {
        String token = registerAndGetToken();
        String tampered = token.substring(0, token.length() - 3) + "aaa";

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }
}
