package pl.delta.crm.property.dictionary;

/**
 * Przeznaczenie lokalu lub hali. Wielowartościowe. Jeden lokal bywa
 * jednocześnie handlowy i usługowy (Otodom: PropertyUseMask, HallDetails: UseMask).
 */
public enum CommercialUse implements Dictionary {

    OFFICE("Biurowy"),
    RETAIL("Handlowy"),
    SERVICE("Usługowy"),
    RESTAURANT("Gastronomiczny"),
    WAREHOUSE("Magazynowy"),
    MANUFACTURING("Produkcyjny"),
    INDUSTRIAL("Przemysłowy"),
    HOTEL("Hotelowy");

    private final String label;

    CommercialUse(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
