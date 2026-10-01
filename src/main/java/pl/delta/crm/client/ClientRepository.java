package pl.delta.crm.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.delta.crm.client.dictionary.ClientStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Każde zapytanie jest zawężone do agencji. Dokładnie jak w
 * {@code PropertyRepository}. Nie ma tu metody potrafiącej zwrócić klienta bez
 * {@code agencyId} (również {@code findById}), żeby nie dało się jej użyć
 * w kontrolerze i wyciekłyby kontakty innego biura.
 */
public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByIdAndAgencyId(UUID id, UUID agencyId);

    Page<Client> findByAgencyId(UUID agencyId, Pageable pageable);

    Page<Client> findByAgencyIdAndStatus(UUID agencyId, ClientStatus status, Pageable pageable);

    /**
     * Kandydaci na duplikat zgłoszenia z formularza. Telefon i e-mail są
     * w bazie znormalizowane ({@code PhoneNumber}, małe litery), więc wołający
     * podaje je w tej samej postaci i wystarcza równość.
     */
    List<Client> findByAgencyIdAndPhone(UUID agencyId, String phone);

    List<Client> findByAgencyIdAndEmail(UUID agencyId, String email);

    /**
     * Wyszukiwanie po imieniu, nazwisku, telefonie lub e-mailu. {@code q} jest
     * już opakowane w {@code %…%} i sprowadzone do małych liter przez serwis.
     * {@code phone} to ta sama fraza bez spacji i myślników. Numer leży w bazie
     * pogrupowany ({@code +48 605 405 932}), a agent wpisuje go po swojemu,
     * więc obie strony porównujemy bez spacji.
     * Wzorce buduje {@link pl.delta.crm.search.SearchTerms}.
     */
    @Query("""
            select c from Client c
            where c.agency.id = :agencyId
              and (lower(c.firstName) like :q
                   or lower(c.lastName) like :q
                   or lower(coalesce(c.email, '')) like :q
                   or replace(coalesce(c.phone, ''), ' ', '') like :phone)
            """)
    Page<Client> search(@Param("agencyId") UUID agencyId, @Param("q") String q,
                        @Param("phone") String phone, Pageable pageable);
}
