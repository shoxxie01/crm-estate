package pl.delta.crm.inquiry;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.delta.crm.inquiry.dto.ConvertInquiryRequest;
import pl.delta.crm.inquiry.dto.ConvertInquiryResponse;
import pl.delta.crm.inquiry.dto.InquiryResponse;
import pl.delta.crm.matching.dto.PropertyMatchResponse;
import pl.delta.crm.security.AppUserPrincipal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Skrzynka zgłoszeń z publicznego formularza. Dla zalogowanych, w zakresie ich biura. */
@RestController
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiries;

    public InquiryController(InquiryService inquiries) {
        this.inquiries = inquiries;
    }

    @GetMapping
    public List<InquiryResponse> list(@RequestParam(defaultValue = "NEW") InquiryStatus status,
                                      @AuthenticationPrincipal AppUserPrincipal principal) {
        return inquiries.list(status, principal.user());
    }

    /** Licznik nowych zgłoszeń do menu bocznego. Lżejszy niż cała lista. */
    @GetMapping("/count")
    public Map<String, Long> count(@AuthenticationPrincipal AppUserPrincipal principal) {
        return Map.of("new", inquiries.countNew(principal.user()));
    }

    @GetMapping("/{id}/matches")
    public List<PropertyMatchResponse> matches(@PathVariable UUID id,
                                               @AuthenticationPrincipal AppUserPrincipal principal) {
        return inquiries.matches(id, principal.user());
    }

    @PostMapping("/{id}/convert")
    public ConvertInquiryResponse convert(@PathVariable UUID id,
                                          @RequestBody(required = false) ConvertInquiryRequest request,
                                          @AuthenticationPrincipal AppUserPrincipal principal) {
        return inquiries.convert(id, request, principal.user());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> reject(@PathVariable UUID id,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        inquiries.reject(id, principal.user());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/intake-link")
    public Map<String, String> intakeLink(@AuthenticationPrincipal AppUserPrincipal principal) {
        return Map.of("token", inquiries.intakeToken(principal.user()));
    }

    @PostMapping("/intake-link/regenerate")
    public Map<String, String> regenerate(@AuthenticationPrincipal AppUserPrincipal principal) {
        return Map.of("token", inquiries.regenerateIntakeToken(principal.user()));
    }
}
