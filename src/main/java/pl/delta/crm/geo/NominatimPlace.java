package pl.delta.crm.geo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Surowa odpowiedź Nominatim. Nie wychodzi poza ten pakiet. Na zewnątrz idzie
 * {@link pl.delta.crm.geo.dto.GeoLocation} z polami nazwanymi tak jak w naszym
 * adresie, żeby zmiana dostawcy geokodowania nie dotykała frontu.
 *
 * <p>Pól jest więcej, niż czytamy; Jackson ignoruje nieznane (domyślna
 * konfiguracja Spring Boota), więc rozbudowa odpowiedzi po ich stronie
 * niczego tu nie psuje.
 */
record NominatimPlace(String lat, String lon,
                      @JsonProperty("display_name") String displayName,
                      Address address) {

    /**
     * Klucze adresu w Nominatim odpowiadają poziomom administracyjnym OSM.
     * Dla Polski: {@code state} to województwo (level 4), {@code county} * powiat (6), {@code municipality} gmina (7). Miejscowość przychodzi
     * pod jednym z {@code city}/{@code town}/{@code village}/{@code hamlet},
     * zależnie od jej wielkości, a dzielnica pod jednym z czterech kluczy
     * poniżej. Stąd łańcuchy „pierwsze niepuste" w {@link GeoService}.
     *
     * <p>Świadomie pomijamy {@code neighbourhood}: bywa nazwą osiedla albo
     * przysiółka, a nie dzielnicy w rozumieniu ogłoszenia, i częściej zaśmiecał
     * pole, niż je trafnie uzupełniał.
     */
    record Address(String state,
                   String county,
                   String municipality,
                   String city,
                   String town,
                   String village,
                   String hamlet,
                   @JsonProperty("city_district") String cityDistrict,
                   String borough,
                   String suburb,
                   String quarter,
                   String road,
                   @JsonProperty("house_number") String houseNumber,
                   String postcode) {
    }
}
