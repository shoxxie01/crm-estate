package pl.delta.crm.inquiry;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.delta.crm.inquiry.dto.IntakeFormResponse;
import pl.delta.crm.inquiry.dto.PublicInquiryRequest;

/**
 * Publiczny formularz „czego szukasz" — jedyne miejsce API dostępne bez
 * logowania poza samym logowaniem (patrz {@code SecurityConfig}). Biuro
 * rozpoznajemy po losowym kluczu z adresu, a odpowiedź nie zdradza niczego
 * poza nazwą biura i treścią zgód.
 */
@RestController
@RequestMapping("/api/public/intake/{token}")
public class PublicIntakeController {

    private final InquiryService inquiries;

    public PublicIntakeController(InquiryService inquiries) {
        this.inquiries = inquiries;
    }

    @GetMapping
    public IntakeFormResponse form(@PathVariable String token) {
        return inquiries.intakeForm(token);
    }

    /** 204 bez treści — także wtedy, gdy zgłoszenie odsiała pułapka na boty. */
    @PostMapping
    public ResponseEntity<Void> submit(@PathVariable String token,
                                       @Valid @RequestBody PublicInquiryRequest request,
                                       HttpServletRequest http) {
        inquiries.submit(token, request, http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
