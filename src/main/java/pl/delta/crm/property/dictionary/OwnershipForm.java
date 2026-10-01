package pl.delta.crm.property.dictionary;

/**
 * Forma własności (Otodom: BuildingOwnership).
 *
 * <p>{@link #PERPETUAL_USUFRUCT} nie występuje w słowniku Otodom, ale jest
 * realną formą na polskim rynku i pojawia się w umowach. Eksport mapuje ją
 * na najbliższą wartość portalu, a my nie tracimy informacji w bazie.
 */
public enum OwnershipForm implements Dictionary {

    FREEHOLD("Pełna własność"),
    COOPERATIVE_OWNERSHIP("Spółdzielcze własnościowe"),
    COOPERATIVE_OWNERSHIP_WITH_REGISTER("Spółdzielcze własnościowe z KW"),
    SHARE("Udział"),
    PERPETUAL_USUFRUCT("Użytkowanie wieczyste");

    private final String label;

    OwnershipForm(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
