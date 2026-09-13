package pl.delta.crm.client.requirement;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Miejscowość z opcjonalną dzielnicą — ten sam podział co w adresie oferty. */
@Embeddable
public class RequirementLocation {

    @Column(name = "city", nullable = false, length = 80)
    private String city;

    @Column(name = "district", length = 64)
    private String district;

    protected RequirementLocation() {
        // wymagane przez JPA
    }

    public RequirementLocation(String city, String district) {
        this.city = city;
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }
}
