package tqs.sparkflow.userservice.exception;

/**
 * Base exception class for all user service exceptions.
 * Provides common exception functionality to avoid code duplication.
 */
public abstract class BaseUserServiceException extends RuntimeException {

  /**
   * Constructor with message.
   *
   * @param message the error message
   */
  protected BaseUserServiceException(String message) {
    super(message);
  }

  /**
   * Constructor with message and cause.
   *
   * @param message the error message
   * @param cause the root cause
   */
  protected BaseUserServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}