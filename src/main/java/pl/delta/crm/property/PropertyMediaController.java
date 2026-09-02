package pl.delta.crm.property;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pl.delta.crm.property.dictionary.MediaType;
import pl.delta.crm.property.dto.MediaResponse;
import pl.delta.crm.property.dto.ReorderMediaRequest;
import pl.delta.crm.property.dto.UpdateCaptionRequest;
import pl.delta.crm.security.AppUserPrincipal;

import java.util.List;
import java.util.UUID;

/**
 * Galeria oferty. Zakres, jak wszędzie, bierze się z tokenu — oferta obcego
 * biura daje 404 już na etapie jej wyszukania, więc do zdjęć nie ma jak dojść.
 */
@RestController
@RequestMapping("/api/properties/{propertyId}/media")
public class PropertyMediaController {

    private final PropertyMediaService mediaService;

    public PropertyMediaController(PropertyMediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping
    public List<MediaResponse> list(@PathVariable UUID propertyId,
                                    @AuthenticationPrincipal AppUserPrincipal principal) {
        return mediaService.list(propertyId, principal.user());
    }

    /**
     * Wgranie jednego lub wielu plików naraz. Agent wraca z oględzin z całym
     * katalogiem zdjęć i zaznacza je jednym ruchem — żądanie na plik oznaczałoby
     * czterdzieści osobnych, z których część mogłaby przepaść.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<MediaResponse> upload(@PathVariable UUID propertyId,
                                      @RequestPart("files") List<MultipartFile> files,
                                      @RequestParam(required = false) MediaType type,
                                      @AuthenticationPrincipal AppUserPrincipal principal) {
        return mediaService.upload(propertyId, files, type, principal.user());
    }

    /** Podmiana samego pliku — pozycja i podpis zostają. */
    @PutMapping("/{mediaId}/file")
    public MediaResponse replaceFile(@PathVariable UUID propertyId,
                                     @PathVariable UUID mediaId,
                                     @RequestPart("file") MultipartFile file,
                                     @AuthenticationPrincipal AppUserPrincipal principal) {
        return mediaService.replaceFile(propertyId, mediaId, file, principal.user());
    }

    @PutMapping("/order")
    public List<MediaResponse> reorder(@PathVariable UUID propertyId,
                                       @Valid @RequestBody ReorderMediaRequest request,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        return mediaService.reorder(propertyId, request.mediaIds(), principal.user());
    }

    @PatchMapping("/{mediaId}")
    public MediaResponse updateCaption(@PathVariable UUID propertyId,
                                       @PathVariable UUID mediaId,
                                       @Valid @RequestBody UpdateCaptionRequest request,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        return mediaService.updateCaption(propertyId, mediaId, request.caption(), principal.user());
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> delete(@PathVariable UUID propertyId,
                                       @PathVariable UUID mediaId,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        mediaService.delete(propertyId, mediaId, principal.user());
        return ResponseEntity.noContent().build();
    }
}
