package pl.delta.crm.client;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import pl.delta.crm.client.dictionary.ClientStatus;
import pl.delta.crm.client.dto.ClientResponse;
import pl.delta.crm.client.dto.ClientSummary;
import pl.delta.crm.client.dto.CreateClientRequest;
import pl.delta.crm.security.AppUserPrincipal;

import java.net.URI;
import java.util.UUID;

/**
 * Zakres widoczności bierze się wyłącznie z tokenu — identyfikator biura nie
 * jest parametrem żądania, tak samo jak przy ofertach.
 */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(
            @Valid @RequestBody CreateClientRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        ClientResponse created = clientService.create(request, principal.user());
        URI location = UriComponentsBuilder.fromPath("/api/clients/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public Page<ClientSummary> list(
            @RequestParam(required = false) ClientStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        return clientService.list(principal.user(), status, search, pageable);
    }

    @GetMapping("/{id}")
    public ClientResponse get(@PathVariable UUID id,
                              @AuthenticationPrincipal AppUserPrincipal principal) {
        return clientService.get(id, principal.user());
    }

    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable UUID id,
                                 @Valid @RequestBody CreateClientRequest request,
                                 @AuthenticationPrincipal AppUserPrincipal principal) {
        return clientService.update(id, request, principal.user());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        clientService.delete(id, principal.user());
        return ResponseEntity.noContent().build();
    }

    /** Przypisanie oferty do klienta jako właściciela. */
    @PutMapping("/{clientId}/properties/{propertyId}")
    public ClientResponse assignProperty(@PathVariable UUID clientId,
                                         @PathVariable UUID propertyId,
                                         @AuthenticationPrincipal AppUserPrincipal principal) {
        return clientService.assignProperty(clientId, propertyId, principal.user());
    }

    @DeleteMapping("/{clientId}/properties/{propertyId}")
    public ResponseEntity<Void> unassignProperty(@PathVariable UUID clientId,
                                                 @PathVariable UUID propertyId,
                                                 @AuthenticationPrincipal AppUserPrincipal principal) {
        clientService.unassignProperty(clientId, propertyId, principal.user());
        return ResponseEntity.noContent().build();
    }
}
