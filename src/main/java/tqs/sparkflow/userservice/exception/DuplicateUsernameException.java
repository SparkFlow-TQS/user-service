package tqs.sparkflow.userservice.exception;

/**
 * Exception thrown when a duplicate username is found.
 */
public class DuplicateUsernameException extends RuntimeException {
  /**
   * Constructor with message.
   *
   * @param message the error message
   */
  public DuplicateUsernameException(String message) {
    super(message);
  }
} 