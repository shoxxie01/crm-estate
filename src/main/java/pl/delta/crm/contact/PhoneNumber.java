package pl.delta.crm.contact;

public final class PhoneNumber {

    /** CRM obsługuje polskie biuro — numer bez kodu kraju jest krajowy. */
    private static final String DEFAULT_COUNTRY_CODE = "48";

    private static final int NATIONAL_DIGITS = 9;

    /** Dłuższy prefiks to już nie kod kraju, tylko numer o innej długości. */
    private static final int MAX_COUNTRY_CODE_DIGITS = 4;

    private PhoneNumber() {
        // klasa narzędziowa
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }

        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String digits = trimmed.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }

        if (!trimmed.startsWith("+")) {
            if (digits.startsWith("00")) {
                // Prefiks wyjścia międzynarodowego znaczy to samo co plus.
                digits = digits.substring(2);
            } else if (digits.length() == NATIONAL_DIGITS) {
                digits = DEFAULT_COUNTRY_CODE + digits;
            }
        }

        if (digits.length() <= NATIONAL_DIGITS) {
            return "+" + digits;
        }

        String countryCode = digits.substring(0, digits.length() - NATIONAL_DIGITS);
        if (countryCode.length() > MAX_COUNTRY_CODE_DIGITS) {
            return "+" + digits;
        }

        String national = digits.substring(digits.length() - NATIONAL_DIGITS);
        return "+" + countryCode + " "
                + national.substring(0, 3) + " "
                + national.substring(3, 6) + " "
                + national.substring(6);
    }
}
