package pl.delta.crm.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.delta.crm.client.Client;
import pl.delta.crm.client.ClientRepository;
import pl.delta.crm.property.Property;
import pl.delta.crm.property.PropertyRepository;
import pl.delta.crm.user.User;

import java.util.UUID;

/**
 * Szybkie wyszukiwanie z nagłówka aplikacji. Jedno żądanie, dwie grupy:
 * klienci i oferty biura. Ostatnio zmieniane najpierw, bo agent zwykle
 * szuka tego, nad czym właśnie pracuje.
 */
@Service
public class SearchService {

    /** Krótsza fraza dopasowałaby pół bazy i nic by nie wniosła. */
    static final int MIN_LENGTH = 2;

    /** Tyle wierszy mieści się w podpowiedziach bez przewijania. */
    static final int LIMIT = 5;

    private final ClientRepository clients;
    private final PropertyRepository properties;

    public SearchService(ClientRepository clients, PropertyRepository properties) {
        this.clients = clients;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(String query, User viewer) {
        String term = query == null ? "" : query.trim();
        if (term.length() < MIN_LENGTH) {
            return SearchResponse.empty();
        }

        UUID agencyId = viewer.getAgency().getId();
        PageRequest top = PageRequest.of(0, LIMIT, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<Client> clientPage = clients.search(
                agencyId, SearchTerms.like(term), SearchTerms.phoneLike(term), top);
        Page<Property> propertyPage = properties.search(agencyId, SearchTerms.like(term), top);

        return new SearchResponse(
                clientPage.map(SearchResponse.ClientHit::from).getContent(),
                clientPage.getTotalElements(),
                propertyPage.map(SearchResponse.PropertyHit::from).getContent(),
                propertyPage.getTotalElements());
    }
}
