package exception;

/**
 * Exception levée par la couche Service (règle métier violée).
 * Unchecked — propagée jusqu'au Controller qui la gère.
 */
public class ServiceException extends RuntimeException {
    public ServiceException(String message) { super(message); }
    public ServiceException(String message, Throwable cause) { super(message, cause); }
}
