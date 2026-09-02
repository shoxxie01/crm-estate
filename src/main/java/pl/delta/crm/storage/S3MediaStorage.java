package pl.delta.crm.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.delta.crm.error.MediaStorageException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/** Implementacja {@link MediaStorage} na API S3 — w dev obsługiwana przez MinIO. */
@Component
public class S3MediaStorage implements MediaStorage {

    private static final Logger log = LoggerFactory.getLogger(S3MediaStorage.class);

    private final S3Client s3;
    private final S3Presigner presigner;
    private final StorageProperties properties;

    public S3MediaStorage(S3Client s3, S3Presigner presigner, StorageProperties properties) {
        this.s3 = s3;
        this.presigner = presigner;
        this.properties = properties;
    }

    /**
     * Bucket zakładamy sami, żeby postawienie projektu było jednym
     * {@code docker compose up} — bez ręcznego klikania w konsoli MinIO.
     *
     * <p>Niepowodzenie nie przerywa startu: storage bywa niedostępny, kiedy ktoś
     * pracuje nad samym frontem, a wtedy ma nie działać galeria, a nie cała
     * aplikacja. Pierwsze wgranie zdjęcia i tak spróbuje ponownie.
     */
    @PostConstruct
    void ensureBucketExists() {
        try {
            createBucketIfMissing();
        } catch (RuntimeException exception) {
            log.warn("Storage niedostępny pod {} — wgrywanie zdjęć nie zadziała, "
                            + "dopóki nie wstanie MinIO (docker compose up -d). Powód: {}",
                    properties.endpoint(), exception.getMessage());
        }
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (NoSuchBucketException exception) {
            // Bucket mógł nie powstać przy starcie, bo MinIO wtedy nie działał.
            createBucketIfMissing();
            s3.putObject(PutObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (SdkException exception) {
            throw new MediaStorageException("Nie udało się zapisać pliku w storage.", exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(key)
                    .build());
        } catch (RuntimeException exception) {
            // Kasowanie leci po commicie transakcji — rzucenie tutaj nie cofnęłoby
            // już usunięcia wiersza, a wywróciłoby odpowiedź dla operacji, która
            // z punktu widzenia użytkownika się udała.
            log.warn("Nie udało się skasować obiektu {}: {}", key, exception.getMessage());
        }
    }

    @Override
    public String url(String key) {
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(properties.urlTtl())
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(key)
                        .build())
                .build();

        return presigner.presignGetObject(request).url().toString();
    }

    private void createBucketIfMissing() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(properties.bucket()).build());
        } catch (NoSuchBucketException missing) {
            s3.createBucket(CreateBucketRequest.builder().bucket(properties.bucket()).build());
            log.info("Utworzono bucket {}", properties.bucket());
        }
    }
}
