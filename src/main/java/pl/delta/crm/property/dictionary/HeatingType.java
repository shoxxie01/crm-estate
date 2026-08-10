package pl.delta.crm.property.dictionary;

/**
 * Ogrzewanie. Wielowartościowe, bo dom potrafi mieć jednocześnie gazowe
 * i kominkowe (Otodom HouseDetails: HeatingMask). Dla mieszkania portal
 * przyjmuje pojedynczą wartość (Heating) — eksport wybiera wtedy pierwszą.
 */
public enum HeatingType implements Dictionary {

    MIEJSKIE("Miejskie"),
    GAZOWE("Gazowe"),
    ELEKTRYCZNE("Elektryczne"),
    WEGLOWE("Węglowe"),
    OLEJOWE("Olejowe"),
    POMPA_CIEPLA("Pompa ciepła"),
    KOLEKTOR_SLONECZNY("Kolektor słoneczny"),
    GEOTERMIKA("Geotermika"),
    BIOMASA("Biomasa"),
    KOMINKOWE("Kominkowe"),
    PIECE_KAFLOWE("Piece kaflowe"),
    INNE("Inne");

    private final String label;

    HeatingType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
