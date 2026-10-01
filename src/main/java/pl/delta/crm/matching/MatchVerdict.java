package pl.delta.crm.matching;

/** Wynik porównania jednego kryterium poszukiwania z ofertą. */
public enum MatchVerdict {

    MET,

    /**
     * Blisko, ale poza zakresem. Np. cena do 10% ponad budżet. Klienci
     * regularnie kupują trochę drożej, niż deklarowali, więc takiej oferty
     * nie ukrywamy, tylko ją oznaczamy.
     */
    NEAR,

    MISSED,

    /** Oferta nie ma danych, żeby to sprawdzić (np. piętra). Agent musi dopytać. */
    UNKNOWN
}
