package pl.delta.crm;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Postgres w kontenerze na czas testów — ta sama wersja co w compose.yaml.
 * {@code @ServiceConnection} podstawia URL, użytkownika i hasło pod
 * autokonfigurację, więc w test/resources/application.yml nie ma datasource'a.
 *
 * <p>Kontener jest zwykłym beanem, więc żyje tyle co kontekst Springa — przy
 * wielu klasach testowych startuje raz i jest współdzielony.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainerConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        // W Testcontainers 2.x klasa nie jest już generyczna (znika self-type <?>).
        return new PostgreSQLContainer("postgres:17-alpine");
    }
}
