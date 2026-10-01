package pl.delta.crm.property.dictionary;

/**
 * Portale ogłoszeniowe, do których eksportujemy oferty.
 *
 * <p>Limity zdjęć różnią się między serwisami. Trzymamy je przy portalu, żeby
 * walidacja przed wysyłką miała się o co oprzeć. Dla Otodom limit wynosi 20
 * zdjęć, każde do 5 MB, min. 400×300 px, format JPEG lub GIF.
 */
public enum Portal implements Dictionary {

    OTODOM("Otodom", 20),
    MORIZON("Morizon", 20),
    NIERUCHOMOSCI_ONLINE("Nieruchomości-online", 25),
    OLX("OLX", 8),
    GRATKA("Gratka", 20);

    private final String label;
    private final int maxPhotos;

    Portal(String label, int maxPhotos) {
        this.label = label;
        this.maxPhotos = maxPhotos;
    }

    @Override
    public String label() {
        return label;
    }

    public int maxPhotos() {
        return maxPhotos;
    }
}
