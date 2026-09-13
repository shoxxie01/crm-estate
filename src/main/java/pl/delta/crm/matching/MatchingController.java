package pl.delta.crm.matching;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pl.delta.crm.matching.dto.ClientMatchResponse;
import pl.delta.crm.matching.dto.PropertyMatchResponse;
import pl.delta.crm.security.AppUserPrincipal;

import java.util.List;
import java.util.UUID;

/**
 * Dopasowania jako osobne zasoby pod ofertą i pod poszukiwaniem — nie są
 * doklejane do ich odpowiedzi, bo liczą się dłużej niż zwykły odczyt, a lista
 * ofert czy klientów ich nie potrzebuje.
 */
@RestController
public class MatchingController {

    private final MatchingService matching;

    public MatchingController(MatchingService matching) {
        this.matching = matching;
    }

    @GetMapping("/api/properties/{propertyId}/matches")
    public List<ClientMatchResponse> forProperty(@PathVariable UUID propertyId,
                                                 @AuthenticationPrincipal AppUserPrincipal principal) {
        return matching.clientsForProperty(propertyId, principal.user());
    }

    @GetMapping("/api/clients/{clientId}/requirements/{requirementId}/matches")
    public List<PropertyMatchResponse> forRequirement(@PathVariable UUID clientId,
                                                     @PathVariable UUID requirementId,
                                                     @AuthenticationPrincipal AppUserPrincipal principal) {
        return matching.propertiesForRequirement(clientId, requirementId, principal.user());
    }
}
