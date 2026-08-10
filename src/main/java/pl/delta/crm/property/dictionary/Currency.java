package pl.delta.crm.property.dictionary;

/** Waluta ceny, czynszu i kaucji (Otodom: PriceCurrency / RentCurrency / DepositCurrency). */
public enum Currency implements Dictionary {

    PLN("zł"),
    EUR("€"),
    USD("$"),
    GBP("£");

    private final String label;

    Currency(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
