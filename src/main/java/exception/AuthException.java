package exception;

/**
 * Exception levée lors d'un échec d'authentification.
 * Unchecked — propagée jusqu'au LoginController.
 */
public class AuthException extends RuntimeException {
    public AuthException(String message) { super(message); }
    public AuthException(String message, Throwable cause) { super(message, cause); }
}
