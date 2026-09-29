package pl.delta.crm.geo.dto;

import pl.delta.crm.property.dictionary.Voivodeship;

/**
 * Adres z formularza, na podstawie którego szukamy punktu na mapie.
 *
 * <p><b>Wymagane są województwo i miejscowość</b> — i tylko one. Sama
 * miejscowość nie wystarcza, bo nazwy się powtarzają (samych „Nowych Wsi" jest
 * w Polsce ponad sto) i pinezka lądowałaby losowo; para z województwem
 * rozstrzyga to w praktyce zawsze. Pozostałe pola jedynie zawężają wynik:
 * kod pocztowy rozdziela miejscowości o tej samej nazwie w jednym województwie,
 * ulica schodzi z centrum miejscowości na konkretną ulicę, a numer budynku —
 * na konkretny budynek.
 */
public record GeoQuery(
        Voivodeship voivodeship,
        String city,
        String district,
        String street,
        String buildingNumber,
        String postalCode) {
}
