package tqs.sparkflow.userservice.exception;

/**
 * Exception thrown when a duplicate email is found.
 */
public class DuplicateEmailException extends BaseUserServiceException {
  /**
   * Constructor with message.
   *
   * @param message the error message
   */
  public DuplicateEmailException(String message) {
    super(message);
  }
} 