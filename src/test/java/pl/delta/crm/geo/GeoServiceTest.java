package pl.delta.crm.geo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.delta.crm.geo.dto.GeoLocation;
import pl.delta.crm.geo.dto.GeoQuery;
import pl.delta.crm.property.dictionary.Voivodeship;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tłumaczenie między nazewnictwem OpenStreetMap a naszym adresem.
 *
 * <p>Bez sieci i bez kontekstu Springa. Sprawdzamy samo mapowanie, bo to ono
 * decyduje, co wpadnie do formularza. Odpowiedzi w testach są przepisane
 * z prawdziwych wywołań Nominatim dla Warszawy, Piaseczna i Krakowa; różnią się
 * one układem kluczy na tyle, że każda pilnuje innej reguły.
 */
class GeoServiceTest {

    @Test
    @DisplayName("Warszawa: dzielnicę bierzemy z suburb, nie z quarter (osiedla)")
    void mapsWarsawDistrict() {
        GeoLocation location = GeoService.toLocation(place("52.2323678", "20.9987276",
                new NominatimPlace.Address(
                        "województwo mazowieckie", null, null,
                        "Warszawa", null, null, null,
                        null, null, "Wola", "Mirów",
                        "Aleja Jana Pawła II", "12", "00-828")));

        assertThat(location.voivodeship()).isEqualTo(Voivodeship.MAZOWIECKIE);
        assertThat(location.city()).isEqualTo("Warszawa");
        assertThat(location.district()).isEqualTo("Wola");
        assertThat(location.street()).isEqualTo("Aleja Jana Pawła II");
        assertThat(location.buildingNumber()).isEqualTo("12");
        assertThat(location.postalCode()).isEqualTo("00-828");
        // OSM nie podaje powiatu dla miasta na prawach powiatu. Dokłada go
        // CityCounties, bo bez niego oferta nie przejdzie przez readyForExport().
        assertThat(location.county()).isEqualTo("Warszawa");
    }

    @Test
    @DisplayName("Piaseczno: z powiatu i gminy zdejmujemy rodzajnik, miejscowość bierzemy z town")
    void stripsAdministrativePrefixes() {
        GeoLocation location = GeoService.toLocation(place("52.07889", "21.0223414",
                new NominatimPlace.Address(
                        "województwo mazowieckie", "powiat piaseczyński", "gmina Piaseczno",
                        null, "Piaseczno", null, null,
                        null, null, null, null,
                        "Wojska Polskiego", "5", "05-500")));

        assertThat(location.county()).isEqualTo("Piaseczyński");
        assertThat(location.commune()).isEqualTo("Piaseczno");
        assertThat(location.city()).isEqualTo("Piaseczno");
    }

    @Test
    @DisplayName("Województwo rozpoznajemy mimo znaków diakrytycznych, myślnika i rodzajnika")
    void matchesVoivodeshipRegardlessOfSpelling() {
        assertThat(voivodeshipOf("województwo kujawsko-pomorskie"))
                .isEqualTo(Voivodeship.KUJAWSKO_POMORSKIE);
        assertThat(voivodeshipOf("Łódzkie")).isEqualTo(Voivodeship.LODZKIE);
        assertThat(voivodeshipOf("województwo świętokrzyskie"))
                .isEqualTo(Voivodeship.SWIETOKRZYSKIE);
        // Nazwa spoza słownika (np. gdy padnie Accept-Language) zostaje pusta,
        // zamiast trafić w przypadkową pozycję listy wyboru.
        assertThat(voivodeshipOf("Masovian Voivodeship")).isNull();
    }

    @Test
    @DisplayName("Miasto na prawach powiatu dostaje powiat i gminę równe swojej nazwie")
    void fillsCountyForCityCounties() {
        // Tak wygląda odpowiedź OSM dla centrum Warszawy: ani powiatu, ani gminy,
        // bo granica miasta sama jest jednostką poziomu powiatu.
        GeoLocation warszawa = GeoService.toLocation(place("52.2297", "21.0122",
                new NominatimPlace.Address(
                        "województwo mazowieckie", null, null,
                        "Warszawa", null, null, null,
                        null, null, "Śródmieście", null,
                        "Marszałkowska", "1", "00-624")));

        assertThat(warszawa.county()).isEqualTo("Warszawa");
        assertThat(warszawa.commune()).isEqualTo("Warszawa");
        assertThat(warszawa.district()).isEqualTo("Śródmieście");
    }

    @Test
    @DisplayName("Gmina miejska: miasto bez `municipality` dostaje gminę o swojej nazwie")
    void fillsCommuneForUrbanGmina() {
        // Puławy: OSM podaje powiat, ale nie gminę, bo granica gminy miejskiej
        // pokrywa się z granicą miasta i jest jednym obiektem.
        GeoLocation location = GeoService.toLocation(place("51.4166", "21.9691",
                new NominatimPlace.Address(
                        "województwo lubelskie", "powiat puławski", null,
                        null, "Puławy", null, null,
                        null, null, null, null,
                        "Lubelska", "2", "24-100")));

        assertThat(location.city()).isEqualTo("Puławy");
        assertThat(location.county()).isEqualTo("Puławski");
        assertThat(location.commune()).isEqualTo("Puławy");
    }

    @Test
    @DisplayName("Wieś bez gminy jej nie dostaje. Brak danych to nie to samo co bycie gminą")
    void doesNotInventCommuneForVillage() {
        GeoLocation location = GeoService.toLocation(place("52.0", "21.0",
                new NominatimPlace.Address(
                        "województwo mazowieckie", "powiat piaseczyński", null,
                        null, null, "Bobrowiec", null,
                        null, null, null, null,
                        null, null, null)));

        assertThat(location.city()).isEqualTo("Bobrowiec");
        assertThat(location.commune()).isNull();
    }

    @Test
    @DisplayName("Ta sama nazwa poza swoim województwem nie dostaje powiatu z automatu")
    void doesNotFillCountyForSameNameElsewhere() {
        // Chełm to miasto na prawach powiatu w lubelskim, ale też wieś
        // w małopolskim. Dopasowanie po samej nazwie dopisałoby wsi powiat,
        // którego nie ma.
        GeoLocation wies = GeoService.toLocation(place("49.9", "20.0",
                new NominatimPlace.Address(
                        "województwo małopolskie", null, null,
                        null, null, "Chełm", null,
                        null, null, null, null,
                        null, null, null)));

        assertThat(wies.city()).isEqualTo("Chełm");
        assertThat(wies.county()).isNull();
        assertThat(wies.commune()).isNull();
    }

    @Test
    @DisplayName("Powiat podany przez OSM ma pierwszeństwo przed listą")
    void osmCountyWins() {
        GeoLocation location = GeoService.toLocation(place("52.2297", "21.0122",
                new NominatimPlace.Address(
                        "województwo mazowieckie", "powiat warszawski", null,
                        "Warszawa", null, null, null,
                        null, null, null, null,
                        null, null, null)));

        assertThat(location.county()).isEqualTo("Warszawski");
    }

    @Test
    @DisplayName("Powiat dwuczłonowy podnosi oba człony, a łączony myślnikiem. Oba po myślniku")
    void capitalizesEveryWord() {
        GeoLocation dwuczlonowy = GeoService.toLocation(place("52.2", "20.6",
                new NominatimPlace.Address(
                        "województwo mazowieckie", "powiat warszawski zachodni", null,
                        null, "Ożarów Mazowiecki", null, null,
                        null, null, null, null, null, null, null)));
        assertThat(dwuczlonowy.county()).isEqualTo("Warszawski Zachodni");

        // Miasta na prawach powiatu nazywają się rzeczownikowo i też mają
        // wyjść poprawnie, mimo że idą inną ścieżką (lista, nie OSM).
        GeoLocation lacznik = GeoService.toLocation(place("49.72", "18.95",
                new NominatimPlace.Address(
                        "województwo śląskie", null, null,
                        "Jastrzębie-Zdrój", null, null, null,
                        null, null, null, null, null, null, null)));
        assertThat(lacznik.county()).isEqualTo("Jastrzębie-Zdrój");
    }

    @Test
    @DisplayName("Lista miast na prawach powiatu ma komplet 66 pozycji")
    void cityCountyListIsComplete() {
        assertThat(CityCounties.count()).isEqualTo(66);
    }

    @Test
    @DisplayName("Gmina ratuje miejscowość, gdy OSM nie poda ani miasta, ani wsi")
    void fallsBackToMunicipality() {
        GeoLocation location = GeoService.toLocation(place("52.0", "21.0",
                new NominatimPlace.Address(
                        "województwo mazowieckie", "powiat grodziski", "gmina Żabia Wola",
                        null, null, null, null,
                        null, null, null, null,
                        null, null, null)));

        assertThat(location.city()).isEqualTo("Żabia Wola");
    }

    @Test
    @DisplayName("Zapytanie składa ulicę z numerem, a dzielnicę dokłada tylko bez ulicy")
    void buildsQuery() {
        assertThat(GeoService.freeFormQuery(new GeoQuery(
                Voivodeship.MALOPOLSKIE, "Kraków", "Stare Miasto", "Floriańska", "3", "31-019")))
                .isEqualTo("Floriańska 3, 31-019 Kraków, województwo małopolskie, Polska");

        // Bez ulicy dzielnica jest jedyną wskazówką węższą niż miasto.
        assertThat(GeoService.freeFormQuery(new GeoQuery(
                Voivodeship.MALOPOLSKIE, "Kraków", "Stare Miasto", null, null, null)))
                .isEqualTo("Stare Miasto, Kraków, województwo małopolskie, Polska");

        // Minimum, przy którym w ogóle szukamy: województwo i miejscowość.
        assertThat(GeoService.freeFormQuery(new GeoQuery(
                Voivodeship.PODLASKIE, "Nowa Wieś", "  ", "", null, null)))
                .isEqualTo("Nowa Wieś, województwo podlaskie, Polska");
    }

    private static Voivodeship voivodeshipOf(String state) {
        return GeoService.toLocation(place("52.0", "21.0",
                new NominatimPlace.Address(state, null, null, "X", null, null, null,
                        null, null, null, null, null, null, null)))
                .voivodeship();
    }

    private static NominatimPlace place(String lat, String lon, NominatimPlace.Address address) {
        return new NominatimPlace(lat, lon, "-", address);
    }
}
