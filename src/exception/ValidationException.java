package exception;

/** Erreur de saisie utilisateur — champ invalide ou manquant. */
public class ValidationException extends RuntimeException {
    private final String champ;

    public ValidationException(String champ, String message) {
        super("[" + champ + "] " + message);
        this.champ = champ;
    }

    public String getChamp() { return champ; }
}
