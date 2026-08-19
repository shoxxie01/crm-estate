package pl.delta.crm.property;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pl.delta.crm.property.dictionary.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Blok finansowy oferty.
 *
 * <p>Czynsz administracyjny jest osobno od ceny, bo portale pokazują go osobno,
 * a przy najmie trzeba jeszcze wiedzieć, czy cena już go zawiera
 * (Otodom: PriceIncludeRent). Każda kwota ma własną walutę — przy ofertach
 * w euro czynsz bywa rozliczany w złotówkach.
 */
@Embeddable
public class Pricing {

    @Column(name = "price", nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_currency", nullable = false, length = 3)
    private Currency priceCurrency = Currency.PLN;

    @Column(name = "price_negotiable", nullable = false)
    private boolean priceNegotiable;

    @Column(name = "rent", precision = 12, scale = 2)
    private BigDecimal rent;

    @Enumerated(EnumType.STRING)
    @Column(name = "rent_currency", length = 3)
    private Currency rentCurrency;

    @Column(name = "price_includes_rent", nullable = false)
    private boolean priceIncludesRent;

    @Column(name = "deposit", precision = 12, scale = 2)
    private BigDecimal deposit;

    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_currency", length = 3)
    private Currency depositCurrency;

    /** Prowizja biura. Dane wewnętrzne — nie idą do ogłoszenia. */
    @Column(name = "commission_percent", precision = 5, scale = 2)
    private BigDecimal commissionPercent;

    /**
     * Cena za metr kwadratowy. W formularzu wpisywana ręcznie albo wyliczana
     * automatycznie z ceny i powierzchni — dlatego trzymamy ją wprost, a nie
     * liczymy dopiero przy odczycie.
     */
    @Column(name = "price_per_m2", precision = 14, scale = 2)
    private BigDecimal pricePerM2;

    protected Pricing() {
        // wymagane przez JPA
    }

    public Pricing(BigDecimal price, Currency priceCurrency) {
        this.price = price;
        this.priceCurrency = priceCurrency;
    }

    /**
     * Cena za metr — portale wyliczają ją same, ale lista ofert w CRM-ie
     * sortuje po niej, więc liczymy ją także u siebie.
     */
    public BigDecimal pricePerSquareMeter(BigDecimal totalArea) {
        if (totalArea == null || totalArea.signum() <= 0) {
            return null;
        }
        return price.divide(totalArea, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Currency getPriceCurrency() {
        return priceCurrency;
    }

    public void setPriceCurrency(Currency priceCurrency) {
        this.priceCurrency = priceCurrency;
    }

    public boolean isPriceNegotiable() {
        return priceNegotiable;
    }

    public void setPriceNegotiable(boolean priceNegotiable) {
        this.priceNegotiable = priceNegotiable;
    }

    public BigDecimal getRent() {
        return rent;
    }

    public Currency getRentCurrency() {
        return rentCurrency;
    }

    public void setRent(BigDecimal rent, Currency currency) {
        this.rent = rent;
        this.rentCurrency = rent == null ? null : currency;
    }

    public boolean isPriceIncludesRent() {
        return priceIncludesRent;
    }

    public void setPriceIncludesRent(boolean priceIncludesRent) {
        this.priceIncludesRent = priceIncludesRent;
    }

    public BigDecimal getDeposit() {
        return deposit;
    }

    public Currency getDepositCurrency() {
        return depositCurrency;
    }

    public void setDeposit(BigDecimal deposit, Currency currency) {
        this.deposit = deposit;
        this.depositCurrency = deposit == null ? null : currency;
    }

    public BigDecimal getCommissionPercent() {
        return commissionPercent;
    }

    public void setCommissionPercent(BigDecimal commissionPercent) {
        this.commissionPercent = commissionPercent;
    }

    public BigDecimal getPricePerM2() {
        return pricePerM2;
    }

    public void setPricePerM2(BigDecimal pricePerM2) {
        this.pricePerM2 = pricePerM2;
    }
}
