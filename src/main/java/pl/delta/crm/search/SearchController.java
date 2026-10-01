package pl.delta.crm.search;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.delta.crm.security.AppUserPrincipal;

/**
 * Szybkie wyszukiwanie. Zakres biura bierze się z tokenu, tak jak w modułach
 * klientów i ofert, więc nie da się trafić w dane innego biura.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @GetMapping
    public SearchResponse search(@RequestParam(name = "q", required = false) String query,
                                 @AuthenticationPrincipal AppUserPrincipal principal) {
        return service.search(query, principal.user());
    }
}
