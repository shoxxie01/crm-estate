package pl.delta.crm.geo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.delta.crm.error.BusinessValidationException;
import pl.delta.crm.geo.dto.GeoLocation;
import pl.delta.crm.geo.dto.GeoQuery;
import pl.delta.crm.property.dictionary.Voivodeship;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Geokodowanie na potrzeby mapy w ofercie. W obie strony.
 *
 * <p>Wymaga zalogowania jak każdy inny endpoint pod {@code /api/**}. Nie ma tu
 * zakresu biura, bo i nie ma czego zawężać: odpowiedź pochodzi z publicznych
 * danych OpenStreetMap i nie dotyczy żadnej oferty. Uwierzytelnienie pilnuje
 * tylko tego, żeby nasz limit u dostawcy nie był publiczną usługą.
 */
@RestController
@RequestMapping("/api/geo")
public class GeoController {

    private final GeoService service;

    public GeoController(GeoService service) {
        this.service = service;
    }

    /**
     * Adres pod pinezką. {@code 204}, gdy pod punktem nie ma adresu. To nie
     * błąd, tylko informacja, że agent trafił w las albo w wodę.
     */
    @GetMapping("/reverse")
    public ResponseEntity<GeoLocation> reverse(@RequestParam double lat, @RequestParam double lon) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (lat < -90 || lat > 90) {
            errors.put("lat", "Szerokość geograficzna musi mieścić się w zakresie od -90 do 90.");
        }
        if (lon < -180 || lon > 180) {
            errors.put("lon", "Długość geograficzna musi mieścić się w zakresie od -180 do 180.");
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }

        GeoLocation location = service.reverse(lat, lon);
        return location == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(location);
    }

    /**
     * Punkty pasujące do adresu, od najlepiej dopasowanego. Pusta lista znaczy
     * „nie znaleziono". Front zostawia wtedy pinezkę tam, gdzie była.
     */
    @GetMapping("/search")
    public List<GeoLocation> search(
            @RequestParam(required = false) Voivodeship voivodeship,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String street,
            @RequestParam(required = false) String buildingNumber,
            @RequestParam(required = false) String postalCode) {

        Map<String, String> errors = new LinkedHashMap<>();
        if (voivodeship == null) {
            errors.put("voivodeship", "Wybierz województwo.");
        }
        if (city == null || city.isBlank()) {
            errors.put("city", "Podaj miejscowość.");
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }

        return service.search(
                new GeoQuery(voivodeship, city, district, street, buildingNumber, postalCode));
    }
}
