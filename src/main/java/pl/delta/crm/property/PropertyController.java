package pl.delta.crm.property;

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
import pl.delta.crm.property.dictionary.PropertyStatus;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.TransactionType;
import pl.delta.crm.property.dto.CreatePropertyRequest;
import pl.delta.crm.property.dto.PropertyResponse;
import pl.delta.crm.property.dto.PropertySummary;
import pl.delta.crm.security.AppUserPrincipal;

import java.net.URI;
import java.util.UUID;

/**
 * Zakres widoczności bierze się wyłącznie z tokenu. Identyfikator biura nie
 * jest parametrem żądania. Gdyby był, wystarczyłoby go podmienić, żeby zobaczyć
 * oferty konkurencji.
 */
@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @PostMapping
    public ResponseEntity<PropertyResponse> create(
            @Valid @RequestBody CreatePropertyRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        PropertyResponse created = propertyService.create(request, principal.user());
        URI location = UriComponentsBuilder.fromPath("/api/properties/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public Page<PropertySummary> list(
            @RequestParam(required = false) PropertyStatus status,
            @RequestParam(required = false) PropertyType type,
            @RequestParam(required = false) TransactionType transaction,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        return propertyService.list(principal.user(), status, type, transaction, pageable);
    }

    @GetMapping("/{id}")
    public PropertyResponse get(@PathVariable UUID id,
                                @AuthenticationPrincipal AppUserPrincipal principal) {
        return propertyService.get(id, principal.user());
    }

    @PutMapping("/{id}")
    public PropertyResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody CreatePropertyRequest request,
                                   @AuthenticationPrincipal AppUserPrincipal principal) {
        return propertyService.update(id, request, principal.user());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        propertyService.delete(id, principal.user());
        return ResponseEntity.noContent().build();
    }
}
