package pl.delta.crm.property.dictionary;

/**
 * Przeznaczenie lokalu lub hali. Wielowartościowe — jeden lokal bywa
 * jednocześnie handlowy i usługowy (Otodom: PropertyUseMask, HallDetails: UseMask).
 */
public enum CommercialUse implements Dictionary {

    BIUROWY("Biurowy"),
    HANDLOWY("Handlowy"),
    USLUGOWY("Usługowy"),
    GASTRONOMICZNY("Gastronomiczny"),
    MAGAZYNOWY("Magazynowy"),
    PRODUKCYJNY("Produkcyjny"),
    PRZEMYSLOWY("Przemysłowy"),
    HOTELOWY("Hotelowy");

    private final String label;

    CommercialUse(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
