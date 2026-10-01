package pl.delta.crm.property.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Nowa kolejność galerii. Komplet identyfikatorów w docelowej kolejności,
 * a nie „przesuń to o jedno w lewo".
 *
 * <p>Stan końcowy zamiast operacji, bo przeciąganie kafelków generuje ich serię,
 * a każda z osobna zostawiałaby galerię w stanie przejściowym. Serwis odrzuca
 * listę, która nie jest dokładną permutacją zdjęć oferty.
 */
public record ReorderMediaRequest(
        @NotEmpty(message = "Podaj kolejność zdjęć.")
        List<UUID> mediaIds
) {
}
