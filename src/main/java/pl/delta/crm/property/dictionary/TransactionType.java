package pl.delta.crm.property.dictionary;

/** Typ oferty (Otodom: OfferType). */
public enum TransactionType implements Dictionary {

    SALE("Sprzedaż"),
    RENT("Wynajem");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
