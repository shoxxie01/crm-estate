package pl.delta.crm.property;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.delta.crm.property.dictionary.PropertyStatus;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.TransactionType;

import java.util.Optional;
import java.util.UUID;

/**
 * Każde zapytanie jest zawężone do agencji.
 *
 * <p>Nie ma tu ani jednej metody, która potrafiłaby zwrócić ofertę bez podania
 * {@code agencyId} — również {@code findById}. To celowe: gdyby taka metoda
 * istniała, prędzej czy później ktoś użyłby jej w kontrolerze i wyciekłyby
 * oferty innego biura.
 */
public interface PropertyRepository extends JpaRepository<Property, UUID> {

    Optional<Property> findByIdAndAgencyId(UUID id, UUID agencyId);

    Page<Property> findByAgencyId(UUID agencyId, Pageable pageable);

    Page<Property> findByAgencyIdAndStatus(UUID agencyId, PropertyStatus status, Pageable pageable);

    Page<Property> findByAgencyIdAndPropertyType(UUID agencyId, PropertyType type, Pageable pageable);

    Page<Property> findByAgencyIdAndTransactionType(UUID agencyId, TransactionType type, Pageable pageable);

    boolean existsByAgencyIdAndReferenceNumber(UUID agencyId, String referenceNumber);

    /** Ile ofert biura ma już numer z danego miesiąca — podstawa kolejnego numeru. */
    long countByAgencyIdAndReferenceNumberStartingWith(UUID agencyId, String prefix);

    long countByAgencyId(UUID agencyId);
}
