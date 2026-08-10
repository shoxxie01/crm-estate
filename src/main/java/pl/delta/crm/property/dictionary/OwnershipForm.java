package pl.delta.crm.property.dictionary;

/**
 * Forma własności (Otodom: BuildingOwnership).
 *
 * <p>{@link #UZYTKOWANIE_WIECZYSTE} nie występuje w słowniku Otodom, ale jest
 * realną formą na polskim rynku i pojawia się w umowach — eksport mapuje ją
 * na najbliższą wartość portalu, a my nie tracimy informacji w bazie.
 */
public enum OwnershipForm implements Dictionary {

    PELNA_WLASNOSC("Pełna własność"),
    SPOLDZIELCZE_WLASNOSCIOWE("Spółdzielcze własnościowe"),
    SPOLDZIELCZE_WLASNOSCIOWE_Z_KW("Spółdzielcze własnościowe z KW"),
    UDZIAL("Udział"),
    UZYTKOWANIE_WIECZYSTE("Użytkowanie wieczyste");

    private final String label;

    OwnershipForm(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
