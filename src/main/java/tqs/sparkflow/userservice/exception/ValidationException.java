package tqs.sparkflow.userservice.exception;

/**
 * Exception thrown when validation fails.
 */
public class ValidationException extends BaseUserServiceException {
  /**
   * Constructor with message.
   *
   * @param message the error message
   */
  public ValidationException(String message) {
    super(message);
  }
} 