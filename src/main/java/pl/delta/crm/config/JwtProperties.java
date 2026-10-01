package pl.delta.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Konfiguracja tokenów. Sekret NIE ma wartości domyślnej celowo. Aplikacja ma
 * się nie wstać bez jawnie ustawionego klucza, zamiast po cichu podpisywać
 * tokeny czymś, co jest w repo.
 *
 * @param secret   klucz HMAC, minimum 32 znaki (256 bitów). Wymóg HS256
 * @param ttl      czas życia tokenu
 * @param issuer   wartość claimu `iss`, weryfikowana przy odczycie
 */
@ConfigurationProperties(prefix = "delta.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer) {

    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "delta.jwt.secret musi mieć co najmniej 32 bajty. Ustaw zmienną środowiskową DELTA_JWT_SECRET.");
        }
        if (ttl == null) {
            ttl = Duration.ofHours(12);
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "delta-crm";
        }
    }
}
