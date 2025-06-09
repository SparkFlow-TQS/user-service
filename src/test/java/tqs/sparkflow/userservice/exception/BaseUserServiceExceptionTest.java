package tqs.sparkflow.userservice.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for BaseUserServiceException.
 */
class BaseUserServiceExceptionTest {

    /**
     * Concrete implementation for testing the abstract base class.
     */
    private static class TestException extends BaseUserServiceException {
        public TestException(String message) {
            super(message);
        }

        public TestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Test
    void testConstructorWithMessage() {
        // Given
        String message = "Test error message";

        // When
        TestException exception = new TestException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void testConstructorWithMessageAndCause() {
        // Given
        String message = "Test error message";
        RuntimeException cause = new RuntimeException("Root cause");

        // When
        TestException exception = new TestException(message, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void testExceptionChaining() {
        // Given
        RuntimeException rootCause = new RuntimeException("Database connection failed");
        String message = "User service operation failed";

        // When
        TestException exception = new TestException(message, rootCause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(rootCause);
        assertThat(exception.getCause().getMessage()).isEqualTo("Database connection failed");
    }
}