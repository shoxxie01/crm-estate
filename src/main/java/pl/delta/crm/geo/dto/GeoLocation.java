package pl.delta.crm.geo.dto;

import pl.delta.crm.property.dictionary.Voivodeship;

import java.math.BigDecimal;

/**
 * Punkt na mapie razem z rozłożonym adresem. Polami dokładnie tymi, które ma
 * formularz oferty, żeby front mógł je przepisać jeden do jednego.
 *
 * <p>{@code voivodeship} jest enumem, a nie tekstem: front wstawia tę wartość
 * do listy wyboru, więc nazwa spoza słownika byłaby dla niego bezużyteczna.
 * Gdy Nominatim zwróci województwo, którego nie umiemy dopasować, pole zostaje
 * {@code null} i wybór należy do agenta.
 */
public record GeoLocation(
        BigDecimal latitude,
        BigDecimal longitude,
        /* Pełny adres jednym ciągiem. Front pokazuje go pod mapą jako potwierdzenie
           tego, co trafiło w pinezkę. */
        String displayName,
        Voivodeship voivodeship,
        String county,
        String commune,
        String city,
        String district,
        String street,
        String buildingNumber,
        String postalCode) {
}
