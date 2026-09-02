package pl.delta.crm.property;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Zdjęcia i pozostałe materiały ofert.
 *
 * <p>W odróżnieniu od {@link PropertyRepository} nie ma tu warunku na agencję —
 * i nie jest to wyjątek od reguły. Do materiału nie da się dojść inaczej niż
 * przez ofertę, a tę pobiera się wyłącznie metodą z {@code agencyId}. Zawężenie
 * jest więc nadal wymuszone, tylko piętro wyżej.
 */
public interface PropertyMediaRepository extends JpaRepository<PropertyMedia, UUID> {

    List<PropertyMedia> findByPropertyIdOrderByPositionAsc(UUID propertyId);

    /**
     * Zdjęcia główne wielu ofert naraz — miniatura w wierszu listy. Jednym
     * zapytaniem, żeby lista nie robiła N+1 na kolekcji zdjęć każdej oferty.
     */
    List<PropertyMedia> findByPropertyIdInAndPosition(Collection<UUID> propertyIds, short position);
}
