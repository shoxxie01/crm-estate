package pl.delta.crm.property;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pl.delta.crm.property.dictionary.BuildingMaterial;
import pl.delta.crm.property.dictionary.BuildingType;
import pl.delta.crm.property.dictionary.ConstructionStatus;
import pl.delta.crm.property.dictionary.GarretType;
import pl.delta.crm.property.dictionary.OwnershipForm;
import pl.delta.crm.property.dictionary.RoofType;
import pl.delta.crm.property.dictionary.Roofing;
import pl.delta.crm.property.dictionary.Surroundings;
import pl.delta.crm.property.dictionary.WindowsType;

/**
 * Parametry budynku. Wszystko opcjonalne — portale nie wymagają żadnego z tych
 * pól, ale każde podniesione zwiększa widoczność oferty w ich filtrach, więc
 * warto mieć gdzie je zapisać od pierwszego dnia.
 */
@Embeddable
public class BuildingDetails {

    @Column(name = "build_year")
    private Short buildYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "building_type", length = 40)
    private BuildingType buildingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "building_material", length = 30)
    private BuildingMaterial buildingMaterial;

    @Enumerated(EnumType.STRING)
    @Column(name = "construction_status", length = 30)
    private ConstructionStatus constructionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "windows_type", length = 20)
    private WindowsType windowsType;

    @Enumerated(EnumType.STRING)
    @Column(name = "roof_type", length = 20)
    private RoofType roofType;

    @Enumerated(EnumType.STRING)
    @Column(name = "roofing", length = 20)
    private Roofing roofing;

    @Enumerated(EnumType.STRING)
    @Column(name = "garret_type", length = 20)
    private GarretType garretType;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_form", length = 40)
    private OwnershipForm ownershipForm;

    @Enumerated(EnumType.STRING)
    @Column(name = "surroundings", length = 20)
    private Surroundings surroundings;

    @Column(name = "furnished")
    private Boolean furnished;

    public Short getBuildYear() {
        return buildYear;
    }

    public void setBuildYear(Short buildYear) {
        this.buildYear = buildYear;
    }

    public BuildingType getBuildingType() {
        return buildingType;
    }

    public void setBuildingType(BuildingType buildingType) {
        this.buildingType = buildingType;
    }

    public BuildingMaterial getBuildingMaterial() {
        return buildingMaterial;
    }

    public void setBuildingMaterial(BuildingMaterial buildingMaterial) {
        this.buildingMaterial = buildingMaterial;
    }

    public ConstructionStatus getConstructionStatus() {
        return constructionStatus;
    }

    public void setConstructionStatus(ConstructionStatus constructionStatus) {
        this.constructionStatus = constructionStatus;
    }

    public WindowsType getWindowsType() {
        return windowsType;
    }

    public void setWindowsType(WindowsType windowsType) {
        this.windowsType = windowsType;
    }

    public RoofType getRoofType() {
        return roofType;
    }

    public void setRoofType(RoofType roofType) {
        this.roofType = roofType;
    }

    public Roofing getRoofing() {
        return roofing;
    }

    public void setRoofing(Roofing roofing) {
        this.roofing = roofing;
    }

    public GarretType getGarretType() {
        return garretType;
    }

    public void setGarretType(GarretType garretType) {
        this.garretType = garretType;
    }

    public OwnershipForm getOwnershipForm() {
        return ownershipForm;
    }

    public void setOwnershipForm(OwnershipForm ownershipForm) {
        this.ownershipForm = ownershipForm;
    }

    public Surroundings getSurroundings() {
        return surroundings;
    }

    public void setSurroundings(Surroundings surroundings) {
        this.surroundings = surroundings;
    }

    public Boolean getFurnished() {
        return furnished;
    }

    public void setFurnished(Boolean furnished) {
        this.furnished = furnished;
    }
}
