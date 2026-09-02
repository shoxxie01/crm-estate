package pl.delta.crm.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.delta.crm.calendar.dictionary.EventStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Jak w pozostałych modułach: nie ma tu ani jednej metody potrafiącej zwrócić
 * wpis bez podania agencji — również {@code findById}. Wyjątkiem jest
 * {@link #findOverlappingForAgent}, które wyszukuje po agencie; agent zawsze
 * należy do jednego biura, więc zakres i tak zostaje zamknięty.
 *
 * <p>Wszystkie zapytania zakresowe dociągają ofertę, klienta i agenta przez
 * {@code join fetch}. Bez tego widok miesiąca robiłby trzy zapytania na każdy
 * wpis — przy pięćdziesięciu terminach to sto pięćdziesiąt strzałów do bazy.
 */
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    @Query("""
            select e from CalendarEvent e
            left join fetch e.property
            left join fetch e.client
            join fetch e.agent
            where e.id = :id and e.agency.id = :agencyId
            """)
    Optional<CalendarEvent> findByIdAndAgencyId(@Param("id") UUID id, @Param("agencyId") UUID agencyId);

    /**
     * Wpisy zachodzące na podany przedział — a nie „zaczynające się w nim".
     * Termin trwający od poniedziałku do środy musi być widoczny również we
     * wtorek, więc warunkiem jest przecięcie zakresów, nie zawieranie.
     */
    @Query("""
            select e from CalendarEvent e
            left join fetch e.property
            left join fetch e.client
            join fetch e.agent
            where e.agency.id = :agencyId
              and e.startsAt < :to
              and e.endsAt > :from
            order by e.startsAt
            """)
    List<CalendarEvent> findInRange(@Param("agencyId") UUID agencyId,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to);

    /** Terminy powiązane z ofertą — sekcja „Terminy" na karcie oferty. */
    @Query("""
            select e from CalendarEvent e
            left join fetch e.property
            left join fetch e.client
            join fetch e.agent
            where e.agency.id = :agencyId and e.property.id = :propertyId
            order by e.startsAt desc
            """)
    List<CalendarEvent> findByProperty(@Param("agencyId") UUID agencyId,
                                       @Param("propertyId") UUID propertyId);

    /** Terminy powiązane z klientem — sekcja „Terminy" na karcie klienta. */
    @Query("""
            select e from CalendarEvent e
            left join fetch e.property
            left join fetch e.client
            join fetch e.agent
            where e.agency.id = :agencyId and e.client.id = :clientId
            order by e.startsAt desc
            """)
    List<CalendarEvent> findByClient(@Param("agencyId") UUID agencyId,
                                     @Param("clientId") UUID clientId);

    /**
     * Odpina terminy od kasowanej oferty.
     *
     * <p>Bez tego oferty z choćby jednym terminem w ogóle nie dało się usunąć —
     * klucz obcy {@code fk_calendar_events_property} nie ma klauzuli
     * {@code ON DELETE}, więc baza odrzucała kasowanie. Terminy zostają, bo
     * historia pokazów jest wartościowa również wtedy, gdy oferta znika
     * z kartoteki; tytuł ma wpisany adres, więc nadal wiadomo, czego dotyczyły.
     *
     * <p>{@code flushAutomatically}, żeby zmiany czekające w kontekście trafiły
     * do bazy przed tym zapytaniem. Bez {@code clearAutomatically}: wyczyszczenie
     * kontekstu odpięłoby encję oferty, którą zaraz potem kasujemy.
     */
    @Modifying(flushAutomatically = true)
    @Query("update CalendarEvent e set e.property = null where e.property.id = :propertyId")
    int detachProperty(@Param("propertyId") UUID propertyId);

    /** To samo dla kasowanego klienta — patrz {@link #detachProperty}. */
    @Modifying(flushAutomatically = true)
    @Query("update CalendarEvent e set e.client = null where e.client.id = :clientId")
    int detachClient(@Param("clientId") UUID clientId);

    /**
     * Terminy tego samego agenta zachodzące na podany przedział — podstawa
     * ostrzeżenia o kolizji. Odwołane pomijamy: nie zajmują już nikomu czasu.
     */
    @Query("""
            select e from CalendarEvent e
            left join fetch e.property
            left join fetch e.client
            join fetch e.agent
            where e.agent.id = :agentId
              and e.status <> :cancelled
              and e.startsAt < :to
              and e.endsAt > :from
            order by e.startsAt
            """)
    List<CalendarEvent> findOverlappingForAgent(@Param("agentId") UUID agentId,
                                                @Param("from") Instant from,
                                                @Param("to") Instant to,
                                                @Param("cancelled") EventStatus cancelled);
}
