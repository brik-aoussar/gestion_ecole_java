package util;

import config.Constantes;
import exception.ValidationException;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Validateurs réutilisables par tous les Services.
 *
 * Deux modes :
 *  - booléen : isValidEmail(str) → true/false (pour tests)
 *  - assertion : assertEmail(str, "champ") → lève ValidationException si invalide
 */
public final class Validator {

    private static final Pattern PATTERN_EMAIL =
            Pattern.compile(Constantes.REGEX_EMAIL, Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_CNE =
            Pattern.compile(Constantes.REGEX_CNE);

    private Validator() {}

    // ══════════════════════════════════════════════════════════════════════════
    //  Méthodes booléennes (is...)
    // ══════════════════════════════════════════════════════════════════════════

    /** Vrai si la chaîne est non nulle et non vide après trim. */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** Vrai si la chaîne contient au moins {@code min} caractères après trim. */
    public static boolean isMinLength(String value, int min) {
        return isNotEmpty(value) && value.trim().length() >= min;
    }

    /** Vrai si l'email respecte le format standard. */
    public static boolean isValidEmail(String email) {
        return isNotEmpty(email) && PATTERN_EMAIL.matcher(email.trim()).matches();
    }

    /** Vrai si la note est dans l'intervalle [0, 20]. */
    public static boolean isValidNote(Double note) {
        return note != null
                && note >= Constantes.NOTE_MIN
                && note <= Constantes.NOTE_MAX;
    }

    /** Vrai si la date est strictement dans le passé. */
    public static boolean isDateInPast(LocalDate date) {
        return date != null && date.isBefore(LocalDate.now());
    }

    /** Vrai si la date est dans le futur ou aujourd'hui. */
    public static boolean isDateInFutureOrToday(LocalDate date) {
        return date != null && !date.isBefore(LocalDate.now());
    }

    /** Vrai si le CNE respecte le format attendu. */
    public static boolean isValidCne(String cne) {
        return isNotEmpty(cne) && PATTERN_CNE.matcher(cne.trim()).matches();
    }

    /** Vrai si le coefficient est strictement positif. */
    public static boolean isValidCoefficient(Double coeff) {
        return coeff != null && coeff > 0;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Méthodes d'assertion (assert...) — lèvent ValidationException
    // ══════════════════════════════════════════════════════════════════════════

    public static void assertNotEmpty(String value, String champ) {
        if (!isNotEmpty(value))
            throw new ValidationException(champ, Constantes.MSG_CHAMP_OBLIGATOIRE);
    }

    public static void assertMinLength(String value, int min, String champ) {
        if (!isMinLength(value, min))
            throw new ValidationException(champ,
                    "Minimum " + min + " caractère(s) requis");
    }

    public static void assertValidEmail(String email, String champ) {
        assertNotEmpty(email, champ);
        if (!isValidEmail(email))
            throw new ValidationException(champ, Constantes.MSG_EMAIL_INVALIDE);
    }

    public static void assertValidNote(Double note, String champ) {
        if (!isValidNote(note))
            throw new ValidationException(champ,
                    Constantes.MSG_NOTE_INVALIDE +
                    " (reçu : " + note + ")");
    }

    public static void assertDateInPast(LocalDate date, String champ) {
        if (!isDateInPast(date))
            throw new ValidationException(champ, Constantes.MSG_DATE_INVALIDE);
    }

    public static void assertNotNull(Object value, String champ) {
        if (value == null)
            throw new ValidationException(champ, Constantes.MSG_CHAMP_OBLIGATOIRE);
    }

    public static void assertValidCoefficient(Double coeff, String champ) {
        if (!isValidCoefficient(coeff))
            throw new ValidationException(champ,
                    "Le coefficient doit être un nombre positif");
    }

    public static void assertValidCne(String cne, String champ) {
        assertNotEmpty(cne, champ);
        if (!isValidCne(cne))
            throw new ValidationException(champ,
                    "Format CNE invalide (ex. AB123456)");
    }

    /**
     * Valide un identifiant Long non nul et > 0.
     * Utilisé pour vérifier les FK avant appel DAO.
     */
    public static void assertValidId(Long id, String champ) {
        if (id == null || id <= 0)
            throw new ValidationException(champ,
                    "Identifiant invalide : " + id);
    }
}
