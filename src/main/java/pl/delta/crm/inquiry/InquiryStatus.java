package pl.delta.crm.inquiry;

import pl.delta.crm.property.dictionary.Dictionary;

/**
 * Stan zgłoszenia. Nie ma „odrzuconego" — odrzucenie usuwa zgłoszenie razem
 * z danymi osobowymi, bo biuro nie ma podstawy, żeby je dalej trzymać.
 */
public enum InquiryStatus implements Dictionary {

    NEW("Nowe"),
    /** Zamienione na klienta z poszukiwaniem (nowego albo istniejącego). */
    CONVERTED("Przyjęte");

    private final String label;

    InquiryStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
