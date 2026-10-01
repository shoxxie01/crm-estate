package pl.delta.crm.search;

import java.util.Locale;

/**
 * Wzorce {@code LIKE} z frazy wpisanej przez agenta.
 *
 * <p>Znaki {@code %} i {@code _} z frazy są maskowane, żeby „50%" szukało
 * dosłownie, a nie „50 i cokolwiek". PostgreSQL bez klauzuli {@code ESCAPE}
 * traktuje backslash jako znak ucieczki, więc wystarcza go poprzedzić.
 */
public final class SearchTerms {

    private SearchTerms() {
        // klasa narzędziowa
    }

    /** {@code %fraza%} małymi literami. Do porównań z {@code lower(...)}. */
    public static String like(String term) {
        return "%" + escape(term.trim().toLowerCase(Locale.ROOT)) + "%";
    }

    /**
     * Wzorzec dla numeru telefonu. Agent wpisuje numer z odstępami lub
     * myślnikami albo bez nich, więc te znikają. Zapytanie usuwa spacje także
     * z numeru w bazie. Fraza bez cyfr zostaje jak w {@link #like(String)}:
     * nie trafi w żaden numer.
     */
    public static String phoneLike(String term) {
        String compact = term.replaceAll("[\\s\\-()]", "");
        return compact.matches(".*\\d.*") ? like(compact) : like(term);
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
