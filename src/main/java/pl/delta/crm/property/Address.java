package pl.delta.crm.property;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pl.delta.crm.property.dictionary.Voivodeship;

import java.math.BigDecimal;

/**
 * Adres nieruchomości.
 *
 * <p>Otodom wymaga kompletu kraj + województwo + powiat + miasto — bez tego
 * odrzuca ogłoszenie niezależnie od pozostałych pól. Dlatego {@code voivodeship},
 * {@code county} i {@code city} są w bazie {@code NOT NULL}.
 */
@Embeddable
public class Address {

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode = "PL";

    @Enumerated(EnumType.STRING)
    @Column(name = "voivodeship", nullable = false, length = 30)
    private Voivodeship voivodeship;

    /**
     * Powiat — tekstem, nie enumem. Uzasadnienie w {@link Voivodeship}.
     *
     * <p>Opcjonalny przy zapisie, choć portal go wymaga: oferta bez powiatu
     * istnieje w CRM-ie, ale nie przejdzie przez {@code readyForExport()}.
     */
    @Column(name = "county", length = 80)
    private String county;

    @Column(name = "commune", length = 80)
    private String commune;

    @Column(name = "city", nullable = false, length = 80)
    private String city;

    /** Dzielnica (Otodom: Quarter). */
    @Column(name = "district", length = 64)
    private String district;

    @Column(name = "street", length = 64)
    private String street;

    @Column(name = "building_number", length = 20)
    private String buildingNumber;

    /** Nigdy nie trafia do ogłoszenia — służy do kontaktu z właścicielem. */
    @Column(name = "apartment_number", length = 20)
    private String apartmentNumber;

    @Column(name = "postal_code", length = 6)
    private String postalCode;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    /**
     * Czy ukryć dokładny adres w ogłoszeniu. Domyślnie tak — właściciele rzadko
     * godzą się na numer budynku w publicznym ogłoszeniu, a odwrotna domyślna
     * wartość oznaczałaby ujawnianie go przez przeoczenie.
     */
    @Column(name = "hide_exact_address", nullable = false)
    private boolean hideExactAddress = true;

    @Column(name = "teryt_simc", length = 7)
    private String terytSimc;

    @Column(name = "teryt_ulic", length = 5)
    private String terytUlic;

    protected Address() {
        // wymagane przez JPA
    }

    public Address(Voivodeship voivodeship, String city) {
        this.voivodeship = voivodeship;
        this.city = city;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public Voivodeship getVoivodeship() {
        return voivodeship;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getCommune() {
        return commune;
    }

    public void setCommune(String commune) {
        this.commune = commune;
    }

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getBuildingNumber() {
        return buildingNumber;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public void setApartmentNumber(String apartmentNumber) {
        this.apartmentNumber = apartmentNumber;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setCoordinates(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public boolean isHideExactAddress() {
        return hideExactAddress;
    }

    public void setHideExactAddress(boolean hideExactAddress) {
        this.hideExactAddress = hideExactAddress;
    }

    public String getTerytSimc() {
        return terytSimc;
    }

    public void setTerytSimc(String terytSimc) {
        this.terytSimc = terytSimc;
    }

    public String getTerytUlic() {
        return terytUlic;
    }

    public void setTerytUlic(String terytUlic) {
        this.terytUlic = terytUlic;
    }
}
