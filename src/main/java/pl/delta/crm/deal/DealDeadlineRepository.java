package pl.delta.crm.deal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.delta.crm.deal.dictionary.DeadlineStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Terminy umowne. Zapytania po transakcji domyka biuro karty; zapytanie
 * kalendarza ({@link #findInRange}) filtruje po biurze samo.
 */
public interface DealDeadlineRepository extends JpaRepository<DealDeadline, UUID> {

    @Query("""
            select d from DealDeadline d
            left join fetch d.movedFrom
            where d.deal.id = :dealId
            order by d.dueDate, d.createdAt
            """)
    List<DealDeadline> findByDeal(@Param("dealId") UUID dealId);

    @Query("""
            select d from DealDeadline d
            where d.id = :id and d.deal.id = :dealId
            """)
    Optional<DealDeadline> findByIdAndDealId(@Param("id") UUID id, @Param("dealId") UUID dealId);

    /** Otwarte terminy wielu kart naraz. Z nich „najbliższy termin" na tablicy. */
    @Query("""
            select d from DealDeadline d
            where d.deal.id in :dealIds and d.status = :open
            order by d.dueDate
            """)
    List<DealDeadline> findOpenForDeals(@Param("dealIds") Collection<UUID> dealIds,
                                        @Param("open") DeadlineStatus open);

    /**
     * Terminy biura w zakresie dat. Warstwa terminów umownych w kalendarzu.
     * Przesunięte pomijamy: w kalendarzu ma stać tylko obowiązująca data.
     */
    @Query("""
            select d from DealDeadline d
            join fetch d.deal deal
            join fetch deal.agent
            where deal.agency.id = :agencyId
              and d.dueDate >= :from
              and d.dueDate < :to
              and d.status <> :moved
            order by d.dueDate
            """)
    List<DealDeadline> findInRange(@Param("agencyId") UUID agencyId,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to,
                                   @Param("moved") DeadlineStatus moved);
}
