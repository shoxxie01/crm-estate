package pl.delta.crm.property.dto;

import pl.delta.crm.property.PropertyMedia;
import pl.delta.crm.property.dictionary.MediaType;

import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Materiał oferty widziany przez front.
 *
 * <p>Nie ma tu {@code storageKey} — front nie ma powodu znać układu bucketa,
 * a znając go, mógłby próbować zgadywać klucze cudzych ofert. Zamiast tego
 * dostaje gotowe, czasowe linki: pełny do podglądu i miniaturę do siatki.
 *
 * @param meetsPortalRequirements czy zdjęcie przejdzie walidację portalu —
 *                                lepiej pokazać to przy kafelku niż dowiedzieć
 *                                się z raportu importu godzinę po wysyłce
 */
public record MediaResponse(
        UUID id,
        MediaType mediaType,
        String fileName,
        short position,
        String caption,
        Integer widthPx,
        Integer heightPx,
        long sizeBytes,
        boolean meetsPortalRequirements,
        String url,
        String thumbnailUrl
) {

    public static MediaResponse from(PropertyMedia media, UnaryOperator<String> signer) {
        return new MediaResponse(
                media.getId(),
                media.getMediaType(),
                media.getFileName(),
                media.getPosition(),
                media.getCaption(),
                media.getWidthPx(),
                media.getHeightPx(),
                media.getSizeBytes(),
                media.meetsPortalRequirements(),
                signer.apply(media.getStorageKey()),
                signer.apply(media.thumbnailKey()));
    }
}
