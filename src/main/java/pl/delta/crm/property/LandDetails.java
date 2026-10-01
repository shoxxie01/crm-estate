package pl.delta.crm.property;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pl.delta.crm.property.dictionary.PlotType;
import pl.delta.crm.property.dictionary.RoadAccess;

/**
 * Parametry działki. Wypełniane dla {@code PLOT}, ale sensowne także przy
 * domu z dużą działką. Dlatego siedzą w tej samej tabeli, a nie w osobnej
 * tabeli podpiętej tylko pod jeden typ.
 */
@Embeddable
public class LandDetails {

    @Enumerated(EnumType.STRING)
    @Column(name = "plot_type", length = 30)
    private PlotType plotType;

    /** Wymiary jako tekst „szer x dł". Otodom przyjmuje dokładnie taki string. */
    @Column(name = "plot_dimensions", length = 32)
    private String dimensions;

    @Enumerated(EnumType.STRING)
    @Column(name = "road_access", length = 20)
    private RoadAccess roadAccess;

    @Column(name = "plot_fenced")
    private Boolean fenced;

    /** Przeznaczenie w miejscowym planie zagospodarowania przestrzennego. */
    @Column(name = "zoning_plan", length = 200)
    private String zoningPlan;

    public PlotType getPlotType() {
        return plotType;
    }

    public void setPlotType(PlotType plotType) {
        this.plotType = plotType;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public RoadAccess getRoadAccess() {
        return roadAccess;
    }

    public void setRoadAccess(RoadAccess roadAccess) {
        this.roadAccess = roadAccess;
    }

    public Boolean getFenced() {
        return fenced;
    }

    public void setFenced(Boolean fenced) {
        this.fenced = fenced;
    }

    public String getZoningPlan() {
        return zoningPlan;
    }

    public void setZoningPlan(String zoningPlan) {
        this.zoningPlan = zoningPlan;
    }
}
