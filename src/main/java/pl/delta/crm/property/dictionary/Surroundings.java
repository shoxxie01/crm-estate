package pl.delta.crm.property.dictionary;

/** Położenie (Otodom HouseDetails: Location). */
public enum Surroundings implements Dictionary {

    MIASTO("Miasto"),
    POD_MIASTEM("Pod miastem"),
    WIES("Wieś");

    private final String label;

    Surroundings(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
