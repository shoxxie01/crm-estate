package pl.delta.crm.deal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Historia etapów. Zakres biura jest zamknięty przez transakcję. O historię
 * pyta się dopiero po wczytaniu karty metodą sprawdzającą biuro.
 */
public interface DealStageChangeRepository extends JpaRepository<DealStageChange, UUID> {

    @Query("""
            select c from DealStageChange c
            join fetch c.changedBy
            where c.deal.id = :dealId
            order by c.changedAt
            """)
    List<DealStageChange> findByDeal(@Param("dealId") UUID dealId);

    /** Cała historia etapów biura, chronologicznie. Do raportu lejka. */
    @Query("""
            select c from DealStageChange c
            where c.deal.agency.id = :agencyId
            order by c.changedAt
            """)
    List<DealStageChange> findByAgency(@Param("agencyId") UUID agencyId);
}
