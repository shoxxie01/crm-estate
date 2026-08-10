package pl.delta.crm.property;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pl.delta.crm.property.dictionary.EnergyClass;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Świadectwo charakterystyki energetycznej.
 *
 * <p>Od 28 kwietnia 2023 r. świadectwo jest w Polsce obowiązkowe przy sprzedaży
 * i wynajmie, a wskaźnik EP musi znaleźć się w treści ogłoszenia. Specyfikacja
 * Otodom Import pochodzi z 2017 r. i tych pól nie zna — obowiązek prawny
 * istnieje jednak niezależnie od formatu XML, a nowsze integracje portali już
 * je przyjmują. Trzymanie ich od początku jest tańsze niż dopisywanie kolumn
 * do tabeli pełnej ofert.
 *
 * <p>Część budynków jest z obowiązku zwolniona (m.in. zabytki wpisane do
 * rejestru, obiekty sakralne, budynki do 50 m²) — stąd {@code exempt} razem
 * z uzasadnieniem, zamiast pustych wskaźników bez wyjaśnienia.
 */
@Embeddable
public class EnergyCertificate {

    /** EP — zapotrzebowanie na nieodnawialną energię pierwotną, kWh/(m²·rok). */
    @Column(name = "energy_ep", precision = 7, scale = 2)
    private BigDecimal energyPrimary;

    /** EK — zapotrzebowanie na energię końcową, kWh/(m²·rok). */
    @Column(name = "energy_ek", precision = 7, scale = 2)
    private BigDecimal energyFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_class", length = 3)
    private EnergyClass energyClass;

    @Column(name = "energy_cert_number", length = 60)
    private String certificateNumber;

    @Column(name = "energy_cert_issued_at")
    private LocalDate issuedAt;

    @Column(name = "energy_cert_valid_until")
    private LocalDate validUntil;

    @Column(name = "energy_cert_exempt", nullable = false)
    private boolean exempt;

    @Column(name = "energy_cert_exempt_note", length = 200)
    private String exemptNote;

    public EnergyCertificate() {
        // świadectwa może jeszcze nie być — pusty obiekt jest poprawnym stanem
    }

    /** Czy ofertę wolno opublikować od strony obowiązku energetycznego. */
    public boolean satisfiesLegalRequirement() {
        return exempt || energyPrimary != null;
    }

    public BigDecimal getEnergyPrimary() {
        return energyPrimary;
    }

    public void setEnergyPrimary(BigDecimal energyPrimary) {
        this.energyPrimary = energyPrimary;
    }

    public BigDecimal getEnergyFinal() {
        return energyFinal;
    }

    public void setEnergyFinal(BigDecimal energyFinal) {
        this.energyFinal = energyFinal;
    }

    public EnergyClass getEnergyClass() {
        return energyClass;
    }

    public void setEnergyClass(EnergyClass energyClass) {
        this.energyClass = energyClass;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public LocalDate getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDate issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public boolean isExempt() {
        return exempt;
    }

    public void setExempt(boolean exempt) {
        this.exempt = exempt;
    }

    public String getExemptNote() {
        return exemptNote;
    }

    public void setExemptNote(String exemptNote) {
        this.exemptNote = exemptNote;
    }
}
