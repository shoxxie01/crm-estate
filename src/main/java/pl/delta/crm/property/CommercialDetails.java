package pl.delta.crm.property;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pl.delta.crm.property.dictionary.Flooring;
import pl.delta.crm.property.dictionary.HallStructure;
import pl.delta.crm.property.dictionary.ParkingType;

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
}
