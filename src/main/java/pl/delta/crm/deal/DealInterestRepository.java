package pl.delta.crm.deal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Zainteresowani. Zakres biura domyka transakcja. O zainteresowanych pyta się
 * dopiero po wczytaniu karty metodą sprawdzającą biuro.
 */
public interface DealInterestRepository extends JpaRepository<DealInterest, UUID> {

    @Query("""
            select i from DealInterest i
            left join fetch i.client
            where i.deal.id = :dealId
            order by i.createdAt
            """)
    List<DealInterest> findByDeal(@Param("dealId") UUID dealId);

    /** Zainteresowani wszystkich kart tablicy naraz. Z nich liczniki na kartach. */
    @Query("""
            select i from DealInterest i
            where i.deal.id in :dealIds
            """)
    List<DealInterest> findByDeals(@Param("dealIds") Collection<UUID> dealIds);

    @Query("""
            select i from DealInterest i
            left join fetch i.client
            where i.id = :id and i.deal.id = :dealId
            """)
    Optional<DealInterest> findByIdAndDealId(@Param("id") UUID id, @Param("dealId") UUID dealId);
}
