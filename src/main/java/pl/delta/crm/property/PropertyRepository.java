package pl.delta.crm.property;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.delta.crm.property.dictionary.PropertyStatus;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.TransactionType;

import java.util.Collection;
import java.util.List;
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

    /** Oferty powierzone przez danego właściciela — na kartę klienta. */
    List<Property> findByOwnerId(UUID ownerId);

    /**
     * Rozkład ofert na typy transakcji dla wielu właścicieli naraz — jednym
     * zapytaniem, żeby lista klientów nie robiła N+1 na kolekcji ofert.
     */
    @Query("""
            select p.owner.id as ownerId, p.transactionType as transactionType, count(p) as count
            from Property p
            where p.owner.id in :ownerIds
            group by p.owner.id, p.transactionType
            """)
    List<OwnerTransactionCount> countByTransactionForOwners(@Param("ownerIds") Collection<UUID> ownerIds);

    /** Kandydaci do dopasowania z poszukiwaniem — kryteria szczegółowe ocenia {@code RequirementMatcher}. */
    @Query("""
            select p from Property p
            where p.agency.id = :agencyId
              and p.transactionType = :transactionType
              and p.propertyType in :propertyTypes
              and p.status in :statuses
            """)
    List<Property> findMatchCandidates(@Param("agencyId") UUID agencyId,
                                       @Param("transactionType") TransactionType transactionType,
                                       @Param("propertyTypes") Collection<PropertyType> propertyTypes,
                                       @Param("statuses") Collection<PropertyStatus> statuses);

    /** Ile ofert biura ma już numer z danego miesiąca — podstawa kolejnego numeru. */
    long countByAgencyIdAndReferenceNumberStartingWith(UUID agencyId, String prefix);

    long countByAgencyId(UUID agencyId);
}
