package pl.delta.crm.deal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.delta.crm.deal.dictionary.DealStage;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Jak w pozostałych modułach: każde zapytanie zwracające transakcję wymaga
 * biura. Odczyty dociągają powiązania przez {@code join fetch}. Tablica
 * pokazuje na każdej karcie klienta, ofertę i agenta, więc bez tego lista
 * pięćdziesięciu kart robiłaby dwieście zapytań.
 */
public interface DealRepository extends JpaRepository<Deal, UUID> {

    @Query("""
            select d from Deal d
            left join fetch d.client
            left join fetch d.property
            left join fetch d.buyer
            join fetch d.agent
            where d.id = :id and d.agency.id = :agencyId
            """)
    Optional<Deal> findByIdAndAgencyId(@Param("id") UUID id, @Param("agencyId") UUID agencyId);

    /**
     * Zawartość tablicy: wszystkie otwarte karty i zamknięte nie starsze niż
     * {@code closedSince}. Zamknięte z całej historii biura zalałyby kolumny
     * „Wygrane" i „Przegrane". Na tablicy mają być widoczne świeże wyniki,
     * a starsze to materiał na raport, nie na Kanban.
     */
    @Query("""
            select d from Deal d
            left join fetch d.client
            left join fetch d.property
            left join fetch d.buyer
            join fetch d.agent
            where d.agency.id = :agencyId
              and (d.stage not in :closed or d.closedAt >= :closedSince)
            order by d.stageChangedAt desc
            """)
    List<Deal> findBoard(@Param("agencyId") UUID agencyId,
                         @Param("closed") Collection<DealStage> closed,
                         @Param("closedSince") Instant closedSince);

    /**
     * Otwarta transakcja tej oferty, jeśli jest. Reguła „jedna otwarta karta
     * na ofertę" (indeks {@code uq_deals_open_property} w V16). Sprawdzamy ją
     * w serwisie, żeby zwrócić czytelny komunikat zamiast 409 z bazy.
     */
    @Query("""
            select d from Deal d
            where d.property.id = :propertyId
              and d.stage not in :closed
            """)
    List<Deal> findOpenForProperty(@Param("propertyId") UUID propertyId,
                                   @Param("closed") Collection<DealStage> closed);

    /**
     * Wszystkie transakcje biura. Materiał na raport lejka. Bez stronicowania,
     * bo raport i tak musi przejść po całej historii (szanse wygranej liczy się
     * z zamkniętych transakcji wszystkich okresów); biuro nieruchomości ma ich
     * setki, nie miliony.
     */
    @Query("""
            select d from Deal d
            join fetch d.agent
            where d.agency.id = :agencyId
            """)
    List<Deal> findAllForReport(@Param("agencyId") UUID agencyId);
}
