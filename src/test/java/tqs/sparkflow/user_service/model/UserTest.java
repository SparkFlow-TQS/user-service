package tqs.sparkflow.user_service.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void whenCreateUser_thenUserHasCorrectFields() {
        User user = new User();
        user.setId("1");
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password123");

        assertThat(user.getId()).isEqualTo("1");
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPassword()).isEqualTo("password123");
    }

    @Test
    void whenUpdateUser_thenUserFieldsAreUpdated() {
        User user = new User();
        user.setId("1");
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password123");

        // Update fields
        user.setUsername("updateduser");
        user.setEmail("updated@example.com");
        user.setPassword("newpassword123");

        assertThat(user.getId()).isEqualTo("1"); // ID should not change
        assertThat(user.getUsername()).isEqualTo("updateduser");
        assertThat(user.getEmail()).isEqualTo("updated@example.com");
        assertThat(user.getPassword()).isEqualTo("newpassword123");
    }
} 