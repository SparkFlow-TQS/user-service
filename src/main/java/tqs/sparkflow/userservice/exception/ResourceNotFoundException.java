package tqs.sparkflow.userservice.exception;

/**
 * Exception thrown when a resource is not found.
 */
public class ResourceNotFoundException extends BaseUserServiceException {
  /**
   * Constructor with message.
   *
   * @param message the error message
   */
  public ResourceNotFoundException(String message) {
    super(message);
  }
} 