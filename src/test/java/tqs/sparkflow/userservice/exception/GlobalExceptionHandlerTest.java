package tqs.sparkflow.userservice.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.Path;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private WebRequest webRequest;

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleConstraintViolationException() {
        // Given
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        when(violation.getMessage()).thenReturn("Validation error");
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(propertyPath.toString()).thenReturn("username");
        
        Set<ConstraintViolation<?>> violations = Set.of(violation);
        ConstraintViolationException exception = new ConstraintViolationException("Constraint violation", violations);

        // When
        ResponseEntity<Object> response = globalExceptionHandler
            .handleConstraintViolationException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).containsKey("username");
        assertThat(body.get("username")).isEqualTo("Validation error");
    }

    @Test
    void testHandleDuplicateUsernameException() {
        // Given
        DuplicateUsernameException exception = new DuplicateUsernameException("Username already exists");

        // When
        ResponseEntity<Object> response = globalExceptionHandler
            .handleDuplicateUsernameException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).containsKey("message");
        assertThat(body.get("message")).isEqualTo("Username already exists");
    }

    @Test
    void testHandleAuthenticationException() {
        // Given
        AuthenticationException exception = new AuthenticationException("Invalid credentials");

        // When
        ResponseEntity<Object> response = globalExceptionHandler
            .handleAuthenticationException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).containsKey("message");
        assertThat(body.get("message")).isEqualTo("Invalid credentials");
    }

    @Test
    void testHandleDuplicateEmailException() {
        // Given
        DuplicateEmailException exception = new DuplicateEmailException("Email already exists");

        // When
        ResponseEntity<Object> response = globalExceptionHandler
            .handleDuplicateEmailException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).containsKey("message");
        assertThat(body.get("message")).isEqualTo("Email already exists");
    }

    @Test
    void testHandleValidationException() {
        // Given
        ValidationException exception = new ValidationException("Validation failed");

        // When
        ResponseEntity<Object> response = globalExceptionHandler
            .handleValidationException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).containsKey("message");
        assertThat(body.get("message")).isEqualTo("Validation failed");
    }

    @Test
    void testHandleMethodArgumentNotValidException() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("userDto", "username", "Username is required");
        FieldError fieldError2 = new FieldError("userDto", "email", "Email is invalid");
        
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ResponseEntity<Object> response = globalExceptionHandler
            .handleMethodArgumentNotValidException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).containsKey("username");
        assertThat(body.get("username")).isEqualTo("Username is required");
        assertThat(body).containsKey("email");
        assertThat(body.get("email")).isEqualTo("Email is invalid");
        assertThat(body).hasSize(2);
    }
}