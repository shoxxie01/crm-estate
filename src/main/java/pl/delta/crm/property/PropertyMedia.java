package pl.delta.crm.property;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pl.delta.crm.property.dictionary.MediaType;

import java.time.Instant;
import java.util.UUID;

/**
 * Zdjęcie lub inny materiał przypięty do oferty.
 *
 * <p>Wymiary i rozmiar pliku trzymamy razem z plikiem, bo portale mają twarde
 * limity i lepiej odrzucić zdjęcie przy wgrywaniu niż dowiedzieć się o problemie
 * z raportu importu godzinę po wysyłce. Otodom: max 20 zdjęć, do 5 MB każde,
 * min. 400×300 px, JPEG lub GIF.
 */
@Entity
@Table(name = "property_media")
public class PropertyMedia {

    /** Ile materiałów wolno przypiąć do jednej oferty. */
    public static final int MAX_PER_PROPERTY = 50;

    /** Minimalna szerokość akceptowana przez Otodom. */
    public static final int MIN_WIDTH_PX = 400;

    /** Minimalna wysokość akceptowana przez Otodom. */
    public static final int MIN_HEIGHT_PX = 300;

    /** Maksymalny rozmiar pojedynczego pliku akceptowany przez Otodom (5 MB). */
    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType;

    @Column(name = "storage_key", nullable = false, length = 300)
    private String storageKey;

    @Column(name = "file_name", nullable = false, length = 120)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 60)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "width_px")
    private Integer widthPx;

    @Column(name = "height_px")
    private Integer heightPx;

    /** Kolejność w galerii; pozycja 0 jest zdjęciem głównym oferty. */
    @Column(name = "position", nullable = false)
    private short position;

    @Column(name = "caption", length = 150)
    private String caption;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected PropertyMedia() {
        // wymagane przez JPA
    }

    public PropertyMedia(MediaType mediaType, String storageKey, String fileName,
                         String contentType, long sizeBytes) {
        this.mediaType = mediaType;
        this.storageKey = storageKey;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    void attachTo(Property property, short position) {
        this.property = property;
        this.position = position;
    }

    /**
     * Klucz obiektu w storage. Biuro siedzi w ścieżce, więc podział na agencje
     * obowiązuje także w warstwie plików. Po kluczu widać, czyj jest plik,
     * a skasowanie całego biura to skasowanie jednego prefiksu.
     *
     * <p>Nazwa pliku jest losowa i nie pochodzi od nazwy wgranej przez użytkownika:
     * ta bywa niepowtarzalna tylko w obrębie jednego folderu na pulpicie agenta,
     * a bywa i taka, której nie da się bezpiecznie wstawić do ścieżki.
     */
    public static String buildStorageKey(UUID agencyId, UUID propertyId) {
        return "agencies/%s/properties/%s/%s%s".formatted(
                agencyId, propertyId, UUID.randomUUID(), ImageProcessor.OUTPUT_EXTENSION);
    }

    /**
     * Klucz miniatury wyprowadzony z klucza pliku. Bez osobnej kolumny. Migracja
     * V3 i tak wymaga, żeby nazwa pliku dała się wyprowadzić z {@code storage_key}
     * (paczka dla Otodom pakuje pliki płasko do ZIP-a).
     */
    public String thumbnailKey() {
        int dot = storageKey.lastIndexOf('.');
        return dot < 0
                ? storageKey + "_thumb"
                : storageKey.substring(0, dot) + "_thumb" + storageKey.substring(dot);
    }

    /**
     * Podmiana samego pliku. Pozycja w galerii, podpis i identyfikator zostają.
     * Agent poprawiający prześwietlone zdjęcie nie powinien tracić kolejności
     * ani opisu, które już ustawił.
     */
    public void replaceFile(String fileName, String contentType, long sizeBytes,
                            Integer widthPx, Integer heightPx) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
    }

    /** Czy plik spełnia wymagania portali. Sprawdzane przed wysyłką paczki. */
    public boolean meetsPortalRequirements() {
        if (mediaType != MediaType.PHOTO) {
            return true;
        }
        return sizeBytes <= MAX_SIZE_BYTES
                && widthPx != null && widthPx >= MIN_WIDTH_PX
                && heightPx != null && heightPx >= MIN_HEIGHT_PX;
    }

    public UUID getId() {
        return id;
    }

    public Property getProperty() {
        return property;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Integer getWidthPx() {
        return widthPx;
    }

    public Integer getHeightPx() {
        return heightPx;
    }

    public void setDimensions(Integer widthPx, Integer heightPx) {
        this.widthPx = widthPx;
        this.heightPx = heightPx;
    }

    public short getPosition() {
        return position;
    }

    public void setPosition(short position) {
        this.position = position;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
