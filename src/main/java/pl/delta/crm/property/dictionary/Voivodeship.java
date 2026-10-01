package pl.delta.crm.property.dictionary;

/**
 * Województwo (Otodom: Province. Pole obowiązkowe).
 *
 * <p>Otodom przyjmuje zarówno swój numer, jak i nazwę tekstową; trzymamy nazwę,
 * bo jest jednoznaczna i niezależna od ich numeracji. Powiat ({@code county})
 * został polem tekstowym celowo. Słownik powiatów Otodom ma kilkaset pozycji,
 * bywa aktualizowany i zawiera nazwy powtarzające się między województwami
 * (jest powiat bielski w śląskim i w podlaskim). Para województwo + nazwa
 * powiatu rozstrzyga to jednoznacznie, a numer i tak trzeba by odświeżać z ich API.
 */
public enum Voivodeship implements Dictionary {

    DOLNOSLASKIE("dolnośląskie"),
    KUJAWSKO_POMORSKIE("kujawsko-pomorskie"),
    LUBELSKIE("lubelskie"),
    LUBUSKIE("lubuskie"),
    LODZKIE("łódzkie"),
    MALOPOLSKIE("małopolskie"),
    MAZOWIECKIE("mazowieckie"),
    OPOLSKIE("opolskie"),
    PODKARPACKIE("podkarpackie"),
    PODLASKIE("podlaskie"),
    POMORSKIE("pomorskie"),
    SLASKIE("śląskie"),
    SWIETOKRZYSKIE("świętokrzyskie"),
    WARMINSKO_MAZURSKIE("warmińsko-mazurskie"),
    WIELKOPOLSKIE("wielkopolskie"),
    ZACHODNIOPOMORSKIE("zachodniopomorskie");

    private final String label;

    Voivodeship(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
