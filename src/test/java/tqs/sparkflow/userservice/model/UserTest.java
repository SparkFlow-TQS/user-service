package tqs.sparkflow.userservice.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @Test
    void whenUsernameIsBlank_thenValidationFails() {
        user.setUsername("");
        var violations = validator.validate(user);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void whenUsernameIsTooShort_thenValidationFails() {
        user.setUsername("ab");
        var violations = validator.validate(user);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void whenUsernameIsTooLong_thenValidationFails() {
        user.setUsername("a".repeat(51));
        var violations = validator.validate(user);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void whenEmailIsBlank_thenValidationFails() {
        user.setEmail("");
        var violations = validator.validate(user);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void whenPasswordIsTooShort_thenValidationFails() {
        user.setPassword("12345");
        var violations = validator.validate(user);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void whenPasswordIsBlank_thenValidationFails() {
        user.setPassword("");
        var violations = validator.validate(user);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }
} 