package exception;

/** Échec d'authentification. */
public class AuthException extends RuntimeException {
    public AuthException(String message) { super(message); }
}
