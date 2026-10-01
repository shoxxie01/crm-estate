package pl.delta.crm.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Dostęp do storage'u obiektowego (MinIO w dev, S3 na produkcji).
 *
 * <p>W odróżnieniu od sekretu JWT te wartości mają sensowne domyślne: bez
 * storage'u aplikacja nadal działa, traci tylko możliwość dodania zdjęcia.
 * Wywalenie startu odcinałoby całą resztę CRM-u z powodu niedziałającej galerii.
 *
 * @param endpoint  adres API S3. Musi być osiągalny także z przeglądarki, bo
 *                  podpis URL-a obejmuje host
 * @param region    MinIO regionów nie ma, ale podpis SigV4 wymaga wpisania jakiegoś
 * @param bucket    jeden bucket na całą instalację; rozdział biur robi klucz obiektu
 * @param urlTtl    ważność podpisanego linku do pliku
 */
@ConfigurationProperties(prefix = "delta.storage")
public record StorageProperties(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        Duration urlTtl
) {

    public StorageProperties {
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "http://localhost:9000";
        }
        if (region == null || region.isBlank()) {
            region = "us-east-1";
        }
        if (bucket == null || bucket.isBlank()) {
            bucket = "delta-crm-media";
        }
        // Dev-owe dane dostępu z compose.yaml, tą samą konwencją co delta/delta
        // przy bazie. Na produkcji nadpisywane zmiennymi środowiskowymi. Bez
        // tego klient S3 nie powstałby nawet tam, gdzie storage nie jest używany.
        if (accessKey == null || accessKey.isBlank()) {
            accessKey = "delta";
        }
        if (secretKey == null || secretKey.isBlank()) {
            secretKey = "delta12345";
        }
        if (urlTtl == null) {
            urlTtl = Duration.ofMinutes(15);
        }
    }
}
