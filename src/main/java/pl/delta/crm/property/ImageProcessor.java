package pl.delta.crm.property;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import pl.delta.crm.error.BusinessValidationException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Normalizacja zdjęcia przy wgrywaniu.
 *
 * <p>Robimy to raz, na wejściu, a nie przy każdej wysyłce na portal: plik z
 * telefonu waży kilkanaście megabajtów i ma orientację zapisaną w EXIF-ie,
 * a portal chce JPEG-a do 5 MB. Przy okazji przekodowanie **usuwa całe EXIF**,
 * w tym współrzędne GPS — zdjęcie z sesji zdradzałoby dokładne położenie
 * nieruchomości nawet wtedy, gdy oferta ma włączone ukrywanie adresu.
 */
@Component
public class ImageProcessor {

    /** Dłuższy bok po normalizacji. Portale i tak nie pokazują większych. */
    private static final int MAX_EDGE_PX = 1920;

    /** Dłuższy bok miniatury — galeria i wiersz listy ofert. */
    private static final int THUMBNAIL_EDGE_PX = 400;

    /** Kompromis rozmiar/jakość; powyżej 0,85 plik rośnie bez widocznego zysku. */
    private static final float JPEG_QUALITY = 0.82f;

    /** Wszystko wychodzi jako JPEG — jeden format w storage i w paczce eksportu. */
    public static final String OUTPUT_CONTENT_TYPE = "image/jpeg";

    public static final String OUTPUT_EXTENSION = ".jpg";

    /**
     * Znormalizowane zdjęcie razem z miniaturą i wymiarami po ewentualnym
     * obrocie z EXIF-u.
     */
    public record ProcessedImage(byte[] full, byte[] thumbnail, int widthPx, int heightPx) {
    }

    public ProcessedImage process(byte[] source, String field) {
        ImageFormat format = ImageFormat.sniff(source);
        if (format == null) {
            throw new BusinessValidationException(Map.of(field,
                    "To nie jest obraz. Dozwolone formaty: JPG, PNG, WEBP, GIF."));
        }
        if (format == ImageFormat.HEIC) {
            throw new BusinessValidationException(Map.of(field,
                    "Format HEIC nie jest obsługiwany. W iPhonie: Ustawienia → Aparat → Formaty → "
                            + "Najbardziej zgodne, albo wyeksportuj zdjęcie jako JPG."));
        }

        BufferedImage oriented = readOriented(source, field);

        if (oriented.getWidth() < PropertyMedia.MIN_WIDTH_PX
                || oriented.getHeight() < PropertyMedia.MIN_HEIGHT_PX) {
            throw new BusinessValidationException(Map.of(field,
                    "Zdjęcie jest za małe (%d×%d px). Portale wymagają co najmniej %d×%d px."
                            .formatted(oriented.getWidth(), oriented.getHeight(),
                                    PropertyMedia.MIN_WIDTH_PX, PropertyMedia.MIN_HEIGHT_PX)));
        }

        BufferedImage flattened = flatten(oriented);
        BufferedImage scaled = downscale(flattened, MAX_EDGE_PX, field);

        return new ProcessedImage(
                toJpeg(scaled, field),
                toJpeg(downscale(flattened, THUMBNAIL_EDGE_PX, field), field),
                scaled.getWidth(),
                scaled.getHeight());
    }

    /**
     * Odczyt z zastosowaniem orientacji z EXIF-u. Sam {@code ImageIO} jej nie
     * czyta, więc zdjęcia z telefonu trzymanego pionowo wychodziłyby położone.
     */
    private BufferedImage readOriented(byte[] source, String field) {
        try {
            return Thumbnails.of(new ByteArrayInputStream(source))
                    .scale(1.0)
                    .asBufferedImage();
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException exception) {
            throw new BusinessValidationException(Map.of(field,
                    "Nie udało się odczytać pliku — może być uszkodzony."));
        }
    }

    /** Nie powiększamy: rozciągnięte 500 px nie stanie się przez to lepszym zdjęciem. */
    private BufferedImage downscale(BufferedImage image, int maxEdge, String field) {
        int longerEdge = Math.max(image.getWidth(), image.getHeight());
        if (longerEdge <= maxEdge) {
            return image;
        }
        try {
            return Thumbnails.of(image)
                    .scale((double) maxEdge / longerEdge)
                    .asBufferedImage();
        } catch (IOException exception) {
            throw new BusinessValidationException(Map.of(field, "Nie udało się przeskalować zdjęcia."));
        }
    }

    /**
     * Przezroczystość na białe tło. JPEG nie ma kanału alfa, a bez tego kroku
     * koder wywala się na PNG z przezroczystością albo daje czarne tło.
     */
    private BufferedImage flatten(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return image;
        }
        BufferedImage opaque = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = opaque.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return opaque;
    }

    private byte[] toJpeg(BufferedImage image, String field) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Thumbnails.of(image)
                    .scale(1.0)
                    .outputFormat("jpg")
                    .outputQuality(JPEG_QUALITY)
                    .toOutputStream(out);
        } catch (IOException exception) {
            throw new BusinessValidationException(Map.of(field, "Nie udało się przetworzyć zdjęcia."));
        }
        return out.toByteArray();
    }

    /**
     * Rozpoznanie formatu po sygnaturze pliku, nie po nagłówku {@code Content-Type}
     * ani rozszerzeniu — jedno i drugie pochodzi od klienta i da się podać dowolne.
     */
    private enum ImageFormat {
        JPEG, PNG, GIF, WEBP, HEIC;

        static ImageFormat sniff(byte[] bytes) {
            if (bytes.length < 12) {
                return null;
            }
            if (starts(bytes, 0xFF, 0xD8, 0xFF)) {
                return JPEG;
            }
            if (starts(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
                return PNG;
            }
            if (starts(bytes, 0x47, 0x49, 0x46, 0x38)) {
                return GIF;
            }
            // RIFF....WEBP — rozmiar kontenera siedzi między jednym a drugim.
            if (starts(bytes, 0x52, 0x49, 0x46, 0x46)
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
                return WEBP;
            }
            // Kontener ISO-BMFF: "ftyp" na pozycji 4, marka formatu zaraz za nim.
            if (bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p') {
                String brand = new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
                if (brand.startsWith("hei") || brand.startsWith("hev") || brand.equals("mif1")) {
                    return HEIC;
                }
            }
            return null;
        }

        private static boolean starts(byte[] bytes, int... signature) {
            for (int i = 0; i < signature.length; i++) {
                if ((bytes[i] & 0xFF) != signature[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    static {
        // Czytamy i piszemy wyłącznie z pamięci — bez tego ImageIO zakłada pliki
        // tymczasowe na dysku dla każdego większego obrazu.
        ImageIO.setUseCache(false);
    }
}
