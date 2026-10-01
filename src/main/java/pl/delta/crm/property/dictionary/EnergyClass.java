package pl.delta.crm.property.dictionary;

/**
 * Klasa energetyczna budynku.
 *
 * <p>Polskie świadectwo charakterystyki energetycznej opiera się na wskaźnikach
 * liczbowych (EP, EK), a nie na literze. Klasa jest wygodnym skrótem używanym
 * w ogłoszeniach i w formatach części portali. Dlatego jest opcjonalna,
 * a źródłem prawdy pozostają wskaźniki.
 */
public enum EnergyClass implements Dictionary {

    A_PLUS("A+"),
    A("A"),
    B("B"),
    C("C"),
    D("D"),
    E("E"),
    F("F"),
    G("G");

    private final String label;

    EnergyClass(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
