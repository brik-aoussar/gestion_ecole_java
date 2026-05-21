package exception;

/**
 * Exception levée pour les erreurs de saisie utilisateur.
 * Unchecked — propagée jusqu'au Controller qui l'affiche.
 */
public class ValidationException extends RuntimeException {
    private final String champ;

    public ValidationException(String champ, String message) {
        super("[" + champ + "] " + message);
        this.champ = champ;
    }

    public String getChamp() { return champ; }
}
