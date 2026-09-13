package pl.delta.crm.client.requirement;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import pl.delta.crm.client.requirement.dto.RequirementRequest;
import pl.delta.crm.client.requirement.dto.RequirementResponse;
import pl.delta.crm.client.requirement.dto.RequirementStatusRequest;
import pl.delta.crm.security.AppUserPrincipal;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Poszukiwania zawsze w kontekście klienta — nie istnieją bez osoby, która szuka. */
@RestController
@RequestMapping("/api/clients/{clientId}/requirements")
public class ClientRequirementController {

    private final ClientRequirementService service;

    public ClientRequirementController(ClientRequirementService service) {
        this.service = service;
    }

    @GetMapping
    public List<RequirementResponse> list(@PathVariable UUID clientId,
                                          @AuthenticationPrincipal AppUserPrincipal principal) {
        return service.list(clientId, principal.user());
    }

    @PostMapping
    public ResponseEntity<RequirementResponse> create(
            @PathVariable UUID clientId,
            @Valid @RequestBody RequirementRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        RequirementResponse created = service.create(clientId, request, principal.user());
        URI location = UriComponentsBuilder.fromPath("/api/clients/{clientId}/requirements/{id}")
                .buildAndExpand(clientId, created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public RequirementResponse update(@PathVariable UUID clientId,
                                      @PathVariable UUID id,
                                      @Valid @RequestBody RequirementRequest request,
                                      @AuthenticationPrincipal AppUserPrincipal principal) {
        return service.update(clientId, id, request, principal.user());
    }

    @PutMapping("/{id}/status")
    public RequirementResponse changeStatus(@PathVariable UUID clientId,
                                            @PathVariable UUID id,
                                            @Valid @RequestBody RequirementStatusRequest request,
                                            @AuthenticationPrincipal AppUserPrincipal principal) {
        return service.changeStatus(clientId, id, request.status(), principal.user());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID clientId,
                                       @PathVariable UUID id,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        service.delete(clientId, id, principal.user());
        return ResponseEntity.noContent().build();
    }
}
