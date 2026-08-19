package pl.delta.crm.property;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pl.delta.crm.property.dictionary.Flooring;
import pl.delta.crm.property.dictionary.HallStructure;
import pl.delta.crm.property.dictionary.ParkingType;

import java.math.BigDecimal;

/**
 * Parametry lokalu użytkowego i hali. Otodom trzyma je w dwóch osobnych tagach
 * (CommercialPropertyDetails, HallDetails), ale zbiory pól w dużej części się
 * pokrywają — rozdzielanie ich u siebie oznaczałoby dwie tabele o niemal
 * identycznej treści.
 */
@Embeddable
public class CommercialDetails {

    @Enumerated(EnumType.STRING)
    @Column(name = "hall_structure", length = 20)
    private HallStructure structure;

    @Enumerated(EnumType.STRING)
    @Column(name = "hall_flooring", length = 20)
    private Flooring flooring;

    @Enumerated(EnumType.STRING)
    @Column(name = "parking_type", length = 20)
    private ParkingType parkingType;

    @Column(name = "has_office_space")
    private Boolean officeSpace;

    @Column(name = "has_social_facilities")
    private Boolean socialFacilities;

    @Column(name = "has_loading_ramp")
    private Boolean loadingRamp;

    /** Moc przyłącza energetycznego [kW] — istotna dla hal i magazynów. */
    @Column(name = "power_connection_kw", precision = 8, scale = 2)
    private BigDecimal powerConnectionKw;

    /** Nośność (dopuszczalne obciążenie) posadzki [t/m²]. */
    @Column(name = "floor_load_t_per_m2", precision = 6, scale = 2)
    private BigDecimal floorLoadPerM2;

    /** Liczba bram / doków rozładunkowych. */
    @Column(name = "loading_docks_count")
    private Short loadingDocksCount;

    public HallStructure getStructure() {
        return structure;
    }

    public void setStructure(HallStructure structure) {
        this.structure = structure;
    }

    public Flooring getFlooring() {
        return flooring;
    }

    public void setFlooring(Flooring flooring) {
        this.flooring = flooring;
    }

    public ParkingType getParkingType() {
        return parkingType;
    }

    public void setParkingType(ParkingType parkingType) {
        this.parkingType = parkingType;
    }

    public Boolean getOfficeSpace() {
        return officeSpace;
    }

    public void setOfficeSpace(Boolean officeSpace) {
        this.officeSpace = officeSpace;
    }

    public Boolean getSocialFacilities() {
        return socialFacilities;
    }

    public void setSocialFacilities(Boolean socialFacilities) {
        this.socialFacilities = socialFacilities;
    }

    public Boolean getLoadingRamp() {
        return loadingRamp;
    }

    public void setLoadingRamp(Boolean loadingRamp) {
        this.loadingRamp = loadingRamp;
    }

    public BigDecimal getPowerConnectionKw() {
        return powerConnectionKw;
    }

    public void setPowerConnectionKw(BigDecimal powerConnectionKw) {
        this.powerConnectionKw = powerConnectionKw;
    }

    public BigDecimal getFloorLoadPerM2() {
        return floorLoadPerM2;
    }

    public void setFloorLoadPerM2(BigDecimal floorLoadPerM2) {
        this.floorLoadPerM2 = floorLoadPerM2;
    }

    public Short getLoadingDocksCount() {
        return loadingDocksCount;
    }

    public void setLoadingDocksCount(Short loadingDocksCount) {
        this.loadingDocksCount = loadingDocksCount;
    }
}
