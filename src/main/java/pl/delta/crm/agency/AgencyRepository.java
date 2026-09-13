package pl.delta.crm.agency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgencyRepository extends JpaRepository<Agency, UUID> {

    /** Biuro po kluczu z adresu publicznego formularza zgłoszeniowego. */
    Optional<Agency> findByIntakeToken(String intakeToken);
}
