package pl.delta.crm.property.dictionary;

/** Rodzaj garażu lub miejsca postojowego. */
public enum GarageType implements Dictionary {

    WOLNOSTOJACY_MUROWANY("Wolnostojący murowany"),
    BLASZANY("Blaszany"),
    PODZIEMNY("Podziemny"),
    MIEJSCE_W_HALI("Miejsce w hali garażowej"),
    MIEJSCE_NAZIEMNE("Miejsce postojowe naziemne");

    private final String label;

    GarageType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
