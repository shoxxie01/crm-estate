package pl.delta.crm.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Jak w pozostałych repozytoriach: każde zapytanie jest zawężone do biura. */
public interface ClientInquiryRepository extends JpaRepository<ClientInquiry, UUID> {

    List<ClientInquiry> findByAgencyIdAndStatusOrderByCreatedAtDesc(UUID agencyId, InquiryStatus status);

    Optional<ClientInquiry> findByIdAndAgencyId(UUID id, UUID agencyId);

    long countByAgencyIdAndStatus(UUID agencyId, InquiryStatus status);
}
