package tqs.sparkflow.userservice.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private Validator validator;
    private User user;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        user = new User("testuser", "test@example.com", "password123");
    }

    @Test
    void whenCreateUser_thenUserIsCreatedWithDefaultValues() {
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPassword()).isEqualTo("password123");
        assertThat(user.isOperator()).isFalse();
    }

    @Test
    void whenCreateUserWithOperator_thenUserIsCreatedWithOperatorTrue() {
        User operatorUser = new User("operator", "operator@example.com", "password123", true);
        assertThat(operatorUser.isOperator()).isTrue();
    }

    @Test
    void whenCreateUserWithNoArgsConstructor_thenUserIsCreatedWithNullValues() {
        User emptyUser = new User();
        assertThat(emptyUser.getUsername()).isNull();
        assertThat(emptyUser.getEmail()).isNull();
        assertThat(emptyUser.getPassword()).isNull();
        assertThat(emptyUser.isOperator()).isFalse();
    }

    private static Stream<Arguments> invalidUserFieldTestCases() {
        return Stream.of(
            Arguments.of("username", "", "blank username"),
            Arguments.of("username", "ab", "username too short"),
            Arguments.of("username", "a".repeat(51), "username too long"),
            Arguments.of("email", "", "blank email"),
            Arguments.of("password", "12345", "password too short"),
            Arguments.of("password", "", "blank password")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidUserFieldTestCases")
    void whenUserFieldIsInvalid_thenValidationFails(String fieldName, String fieldValue, String description) {
        // Given
        switch (fieldName) {
            case "username":
                user.setUsername(fieldValue);
                break;
            case "email":
                user.setEmail(fieldValue);
                break;
            case "password":
                user.setPassword(fieldValue);
                break;
            default:
                throw new IllegalArgumentException("Unknown field: " + fieldName);
        }

        // When
        var violations = validator.validate(user);

        // Then
        assertThat(violations)
            .isNotEmpty()
            .anyMatch(v -> v.getPropertyPath().toString().equals(fieldName));
    }

} 