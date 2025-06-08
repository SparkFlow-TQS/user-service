package tqs.sparkflow.userservice.exception;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends RuntimeException {
  /**
   * Constructor with message.
   *
   * @param message the error message
   */
  public AuthenticationException(String message) {
    super(message);
  }
} 