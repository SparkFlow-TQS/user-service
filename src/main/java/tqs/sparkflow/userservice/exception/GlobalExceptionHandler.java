package tqs.sparkflow.userservice.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for the user service.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles validation errors for request body validation.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex, WebRequest request) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
  }

  /**
   * Handles constraint violation exceptions.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolationException(
      ConstraintViolationException ex, WebRequest request) {
    Map<String, String> errors = new HashMap<>();
    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      String fieldName = violation.getPropertyPath().toString();
      String errorMessage = violation.getMessage();
      errors.put(fieldName, errorMessage);
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
  }

  /**
   * Handles validation exceptions.
   */
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<Object> handleValidationException(
      ValidationException ex, WebRequest request) {
    Map<String, String> error = new HashMap<>();
    error.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * Handles authentication exceptions.
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Object> handleAuthenticationException(
      AuthenticationException ex, WebRequest request) {
    Map<String, String> error = new HashMap<>();
    error.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  /**
   * Handles duplicate email exceptions.
   */
  @ExceptionHandler(DuplicateEmailException.class)
  public ResponseEntity<Object> handleDuplicateEmailException(
      DuplicateEmailException ex, WebRequest request) {
    Map<String, String> error = new HashMap<>();
    error.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  /**
   * Handles duplicate username exceptions.
   */
  @ExceptionHandler(DuplicateUsernameException.class)
  public ResponseEntity<Object> handleDuplicateUsernameException(
      DuplicateUsernameException ex, WebRequest request) {
    Map<String, String> error = new HashMap<>();
    error.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }
}