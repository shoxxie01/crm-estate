package pl.delta.crm.geo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Konfiguracja geokodowania (OpenStreetMap / Nominatim).
 *
 * @param nominatimUrl  adres instancji Nominatim bez końcowego ukośnika
 * @param userAgent     nagłówek {@code User-Agent} — polityka OSM wymaga, żeby
 *                      jednoznacznie wskazywał aplikację i sposób kontaktu;
 *                      anonimowe żądania bywają blokowane
 * @param timeout       limit na połączenie i na odczyt
 * @param minInterval   minimalny odstęp między żądaniami do Nominatim (polityka
 *                      publicznej instancji: najwyżej jedno na sekundę)
 * @param cacheSize     ile ostatnich odpowiedzi trzymamy w pamięci
 */
@ConfigurationProperties(prefix = "delta.geo")
public record GeoProperties(
        String nominatimUrl,
        String userAgent,
        Duration timeout,
        Duration minInterval,
        int cacheSize) {

    public GeoProperties {
        if (nominatimUrl == null || nominatimUrl.isBlank()) {
            nominatimUrl = "https://nominatim.openstreetmap.org";
        }
        nominatimUrl = nominatimUrl.replaceAll("/+$", "");

        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "delta-crm/1.0";
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(5);
        }
        if (minInterval == null) {
            minInterval = Duration.ofSeconds(1);
        }
        if (cacheSize <= 0) {
            cacheSize = 500;
        }
    }
}
