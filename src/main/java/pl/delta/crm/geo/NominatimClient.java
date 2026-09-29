package pl.delta.crm.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.delta.crm.error.GeoUnavailableException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dostęp do Nominatim — jedyne miejsce w aplikacji, które wychodzi do OSM.
 *
 * <p><b>Dlaczego przez backend, a nie prosto z przeglądarki.</b> Nominatim
 * pozwala na ruch z przeglądarki (wysyła nagłówki CORS), ale jego polityka
 * użycia stawia trzy warunki, których front nie jest w stanie spełnić:
 * {@code User-Agent} jednoznacznie wskazujący aplikację (przeglądarka nie daje
 * go nadpisać), najwyżej jedno żądanie na sekundę <i>z całej instalacji</i>
 * oraz cache'owanie powtórzeń. Biuro siedzi za jednym adresem IP, więc pięciu
 * agentów przeciągających pinezkę jednocześnie zostałoby wspólnie odcięte.
 * Z proxy limit i cache są wspólne i policzalne, a podmiana geokodera na
 * własną instancję albo inny serwis to zmiana jednego adresu w konfiguracji.
 */
@Component
public class NominatimClient {

    private static final Logger log = LoggerFactory.getLogger(NominatimClient.class);

    /**
     * Ile najdłużej żądanie czeka w kolejce na swoje okno czasowe. Po
     * przekroczeniu odpowiadamy błędem zamiast trzymać wątek Tomcata: mapa jest
     * dodatkiem do formularza i lepiej, żeby powiedziała „spróbuj ponownie", niż
     * żeby wyczerpała pulę wątków obsługujących resztę CRM-u.
     */
    private static final long MAX_QUEUE_WAIT_NANOS = 3_000_000_000L;

    private final RestClient http;
    private final GeoProperties properties;

    /** Najbliższa wolna chwila, w której wolno wykonać żądanie (System.nanoTime). */
    private final Object gate = new Object();
    private long nextSlot = System.nanoTime();

    /**
     * LRU na odpowiedzi. Powtórzeń jest tu sporo: agent przeciąga pinezkę tam
     * i z powrotem, a edycja tej samej oferty pyta o ten sam adres przy każdym
     * wejściu w formularz. Cache w pamięci procesu wystarcza — dane adresowe
     * zmieniają się w skali miesięcy, a przy restarcie nie tracimy niczego
     * poza kilkoma milisekundami.
     */
    private final Map<String, List<NominatimPlace>> cache;

    public NominatimClient(GeoProperties properties) {
        this.properties = properties;
        this.cache = Collections.synchronizedMap(
                new LinkedHashMap<>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, List<NominatimPlace>> eldest) {
                        return size() > properties.cacheSize();
                    }
                });

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.timeout());
        factory.setReadTimeout(properties.timeout());

        this.http = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", properties.userAgent())
                .defaultHeader("Accept", "application/json")
                // Nominatim odpowiada po polsku, gdy go o to poprosić — inaczej
                // wraca „Masovian Voivodeship" i nie trafia w nasz słownik.
                .defaultHeader("Accept-Language", "pl")
                .build();
    }

    /** Adres pod wskazanym punktem. Pusta lista, gdy w tym miejscu nic nie ma (np. środek morza). */
    List<NominatimPlace> reverse(double latitude, double longitude) {
        String query = params(
                "lat", format(latitude),
                "lon", format(longitude),
                "format", "jsonv2",
                "addressdetails", "1",
                // 18 = poziom budynku. Niżej Nominatim gubi numer domu, wyżej
                // zwraca sam obszar administracyjny bez ulicy.
                "zoom", "18");
        return fetch("/reverse", query, true);
    }

    /** Punkty pasujące do zapytania tekstowego, od najlepiej dopasowanego. */
    List<NominatimPlace> search(String freeFormQuery, int limit) {
        String query = params(
                "q", freeFormQuery,
                "format", "jsonv2",
                "addressdetails", "1",
                "countrycodes", "pl",
                "limit", Integer.toString(limit));
        return fetch("/search", query, false);
    }

    /**
     * @param single {@code /reverse} odpowiada pojedynczym obiektem, {@code /search}
     *               tablicą — tu obie postaci sprowadzamy do listy.
     */
    private List<NominatimPlace> fetch(String path, String query, boolean single) {
        String key = path + "?" + query;

        List<NominatimPlace> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        awaitSlot();

        List<NominatimPlace> result;
        try {
            RestClient.ResponseSpec response = http.get()
                    .uri(URI.create(properties.nominatimUrl() + key))
                    .retrieve();

            if (single) {
                NominatimPlace place = response.body(NominatimPlace.class);
                // Punkt bez adresu (środek morza, nieznany kraj) wraca jako
                // {"error": …} — po zmapowaniu na rekord zostaje z pustym lat.
                result = place == null || place.lat() == null ? List.of() : List.of(place);
            } else {
                NominatimPlace[] places = response.body(NominatimPlace[].class);
                result = places == null ? List.of() : List.of(places);
            }
        } catch (RestClientException exception) {
            log.warn("Nominatim nie odpowiedział na {}: {}", path, exception.getMessage());
            throw new GeoUnavailableException(
                    "Nie udało się połączyć z usługą map. Adres i pinezkę można ustawić ręcznie.", exception);
        }

        cache.put(key, result);
        return result;
    }

    /**
     * Rezerwuje najbliższe wolne okno czasowe i czeka na nie. Rezerwacja jest
     * pod zamkiem, samo czekanie już nie — inaczej wątki blokowałyby się
     * nawzajem na czas snu i kolejka rosłaby dwa razy szybciej, niż wynika
     * z limitu.
     */
    private void awaitSlot() {
        long wait;
        synchronized (gate) {
            long now = System.nanoTime();
            long slot = Math.max(now, nextSlot);
            wait = slot - now;
            if (wait > MAX_QUEUE_WAIT_NANOS) {
                throw new GeoUnavailableException(
                        "Usługa map jest chwilowo przeciążona. Spróbuj za moment.");
            }
            nextSlot = slot + properties.minInterval().toNanos();
        }

        if (wait > 0) {
            try {
                Thread.sleep(wait / 1_000_000L, (int) (wait % 1_000_000L));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new GeoUnavailableException("Przerwano oczekiwanie na usługę map.", interrupted);
            }
        }
    }

    /** Sześć miejsc po przecinku to ok. 10 cm — więcej nie ma znaczenia dla adresu. */
    private static String format(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static String params(String... pairs) {
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append(pairs[i]).append('=')
                    .append(URLEncoder.encode(pairs[i + 1], StandardCharsets.UTF_8));
        }
        return query.toString();
    }
}
