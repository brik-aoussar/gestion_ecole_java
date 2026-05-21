package exception;

/**
 * Exception levée par la couche Service (règle métier violée).
 * Checked — encapsule les DAOException et erreurs métier.
 */
public class ServiceException extends Exception {
    public ServiceException(String message) { super(message); }
    public ServiceException(String message, Throwable cause) { super(message, cause); }
}
