package pl.delta.crm.property.dictionary;

/** Położenie (Otodom HouseDetails: Location). */
public enum Surroundings implements Dictionary {

    CITY("Miasto"),
    SUBURBS("Pod miastem"),
    COUNTRYSIDE("Wieś");

    private final String label;

    Surroundings(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
