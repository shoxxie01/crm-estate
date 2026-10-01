package pl.delta.crm.client.requirement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.delta.crm.client.dictionary.RequirementStatus;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.TransactionType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Jak w pozostałych repozytoriach: każde zapytanie jest zawężone do biura. */
public interface ClientRequirementRepository extends JpaRepository<ClientRequirement, UUID> {

    List<ClientRequirement> findByClientIdAndAgencyId(UUID clientId, UUID agencyId);

    Optional<ClientRequirement> findByIdAndClientIdAndAgencyId(UUID id, UUID clientId, UUID agencyId);

    /**
     * Kandydaci do dopasowania z ofertą: aktywne poszukiwania biura z tą samą
     * transakcją i obejmujące dany rodzaj nieruchomości. Resztę kryteriów ocenia
     * {@code RequirementMatcher}, bo musi umieć wyjaśnić wynik, a nie tylko go podać.
     */
    @Query("""
            select distinct r from ClientRequirement r join r.propertyTypes t
            where r.agency.id = :agencyId
              and r.status = :status
              and r.transactionType = :transactionType
              and t = :propertyType
            """)
    List<ClientRequirement> findMatchCandidates(
            @Param("agencyId") UUID agencyId,
            @Param("status") RequirementStatus status,
            @Param("transactionType") TransactionType transactionType,
            @Param("propertyType") PropertyType propertyType);

    /**
     * Rozkład poszukiwań w danym stanie na typy transakcji, dla całej strony
     * listy klientów jednym zapytaniem. Odpowiednik
     * {@code PropertyRepository.countByTransactionForOwners}.
     */
    @Query("""
            select r.client.id as clientId, r.transactionType as transactionType, count(r) as count
            from ClientRequirement r
            where r.client.id in :clientIds and r.status = :status
            group by r.client.id, r.transactionType
            """)
    List<RequirementTransactionCount> countByTransactionForClients(
            @Param("clientIds") Collection<UUID> clientIds,
            @Param("status") RequirementStatus status);
}
