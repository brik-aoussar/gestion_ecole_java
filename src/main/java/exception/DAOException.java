package exception;

/**
 * Exception levée par la couche DAO (erreur SQL / accès données).
 * Checked — force la gestion explicite dans les services.
 */
public class DAOException extends Exception {
    private final String operation;

    public DAOException(String operation, String message) {
        super("[DAO:" + operation + "] " + message);
        this.operation = operation;
    }

    public DAOException(String operation, String message, Throwable cause) {
        super("[DAO:" + operation + "] " + message, cause);
        this.operation = operation;
    }

    public String getOperation() { return operation; }
}
