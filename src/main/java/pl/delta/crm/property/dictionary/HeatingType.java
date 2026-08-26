package pl.delta.crm.property.dictionary;

/**
 * Ogrzewanie. Wielowartościowe, bo dom potrafi mieć jednocześnie gazowe
 * i kominkowe (Otodom HouseDetails: HeatingMask). Dla mieszkania portal
 * przyjmuje pojedynczą wartość (Heating) — eksport wybiera wtedy pierwszą.
 */
public enum HeatingType implements Dictionary {

    DISTRICT("Miejskie"),
    GAS("Gazowe"),
    ELECTRIC("Elektryczne"),
    COAL("Węglowe"),
    OIL("Olejowe"),
    HEAT_PUMP("Pompa ciepła"),
    SOLAR_COLLECTOR("Kolektor słoneczny"),
    GEOTHERMAL("Geotermika"),
    BIOMASS("Biomasa"),
    FIREPLACE("Kominkowe"),
    TILED_STOVE("Piece kaflowe"),
    OTHER("Inne");

    private final String label;

    HeatingType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
