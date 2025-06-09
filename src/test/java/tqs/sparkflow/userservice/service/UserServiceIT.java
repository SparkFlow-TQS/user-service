package tqs.sparkflow.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import tqs.sparkflow.userservice.UserServiceApplication;
import tqs.sparkflow.userservice.config.TestConfig;
import tqs.sparkflow.userservice.config.TestcontainersConfiguration;
import tqs.sparkflow.userservice.dto.UserCreateDto;
import tqs.sparkflow.userservice.dto.UserUpdateDto;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.DuplicateUsernameException;
import tqs.sparkflow.userservice.exception.ResourceNotFoundException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.repository.UserRepository;

@SpringBootTest(
    classes = {
        UserServiceApplication.class,
        TestConfig.class,
        TestcontainersConfiguration.class
    },
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.data.mongodb.auto-index-creation=true"
    }
)
@ActiveProfiles("test")
@Testcontainers
class UserServiceIT {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        testUser = userRepository.save(testUser);
    }

    @Test
    void whenCreateUserWithValidData_thenReturnUser() {
        UserCreateDto userCreateDTO = new UserCreateDto();
        userCreateDTO.setUsername("newuser");
        userCreateDTO.setEmail("newuser@example.com");
        userCreateDTO.setPassword("password123");

        User createdUser = userService.createUser(userCreateDTO);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getUsername()).isEqualTo("newuser");
        assertThat(createdUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(createdUser.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", createdUser.getPassword())).isTrue();
    }

    @Test
    void whenCreateUserWithExistingEmail_thenThrowException() {
        UserCreateDto userCreateDTO = new UserCreateDto();
        userCreateDTO.setUsername("anotheruser");
        userCreateDTO.setEmail("test@example.com"); // Same email as testUser
        userCreateDTO.setPassword("password123");

        assertThatThrownBy(() -> userService.createUser(userCreateDTO))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void whenCreateUserWithExistingUsername_thenThrowException() {
        UserCreateDto userCreateDTO = new UserCreateDto();
        userCreateDTO.setUsername("testuser"); // Same username as testUser
        userCreateDTO.setEmail("another@example.com");
        userCreateDTO.setPassword("password123");

        assertThatThrownBy(() -> userService.createUser(userCreateDTO))
                .isInstanceOf(DuplicateUsernameException.class);
    }

    @Test
    void whenGetUserById_thenReturnUser() {
        User foundUser = userService.getUserById(testUser.getId());

        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(testUser.getId());
        assertThat(foundUser.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(foundUser.getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    void whenGetUserByNonExistentId_thenThrowException() {
        String nonExistentId = "507f1f77bcf86cd799439011";

        assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void whenGetUserByEmail_thenReturnUser() {
        User foundUser = userService.getUserByEmail(testUser.getEmail());

        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(testUser.getId());
        assertThat(foundUser.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(foundUser.getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    void whenGetUserByNonExistentEmail_thenThrowException() {
        assertThatThrownBy(() -> userService.getUserByEmail("nonexistent@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void whenGetAllUsers_thenReturnUserList() {
        // Create additional users
        User user2 = new User("user2", "user2@example.com", passwordEncoder.encode("password123"));
        User user3 = new User("user3", "user3@example.com", passwordEncoder.encode("password123"));
        userRepository.saveAll(List.of(user2, user3));

        List<User> users = userService.getAllUsers();

        assertThat(users).hasSize(3);
        assertThat(users).extracting(User::getUsername)
                .containsExactlyInAnyOrder("testuser", "user2", "user3");
    }

    @Test
    void whenUpdateUserWithValidData_thenReturnUpdatedUser() {
        UserUpdateDto updateUserDTO = new UserUpdateDto();
        updateUserDTO.setUsername("updateduser");
        updateUserDTO.setEmail("updated@example.com");
        updateUserDTO.setPassword("newpassword123");

        User updatedUser = userService.updateUser(testUser.getId(), updateUserDTO);

        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getId()).isEqualTo(testUser.getId());
        assertThat(updatedUser.getUsername()).isEqualTo("updateduser");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getPassword()).isNotEqualTo("newpassword123");
        assertThat(passwordEncoder.matches("newpassword123", updatedUser.getPassword())).isTrue();
    }

    @Test
    void whenUpdateUserWithExistingEmail_thenThrowException() {
        // Create another user
        User anotherUser = new User("anotheruser", "another@example.com", passwordEncoder.encode("password123"));
        userRepository.save(anotherUser);

        UserUpdateDto updateUserDTO = new UserUpdateDto();
        updateUserDTO.setUsername("testuser");
        updateUserDTO.setEmail("another@example.com"); // Try to use existing email
        updateUserDTO.setPassword("password123");

        String userId = testUser.getId();
        assertThatThrownBy(() -> userService.updateUser(userId, updateUserDTO))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void whenUpdateUserWithExistingUsername_thenThrowException() {
        // Create another user
        User anotherUser = new User("anotheruser", "another@example.com", passwordEncoder.encode("password123"));
        userRepository.save(anotherUser);

        UserUpdateDto updateUserDTO = new UserUpdateDto();
        updateUserDTO.setUsername("anotheruser"); // Try to use existing username

        String userId = testUser.getId();
        assertThatThrownBy(() -> userService.updateUser(userId, updateUserDTO))
                .isInstanceOf(DuplicateUsernameException.class);
    }

    @Test
    void whenUpdateNonExistentUser_thenThrowException() {
        String nonExistentId = "507f1f77bcf86cd799439011";
        UserUpdateDto updateUserDTO = new UserUpdateDto();
        updateUserDTO.setUsername("newusername");
        updateUserDTO.setEmail("new@example.com");
        updateUserDTO.setPassword("password123");

        assertThatThrownBy(() -> userService.updateUser(nonExistentId, updateUserDTO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void whenDeleteExistingUser_thenUserIsDeleted() {
        userService.deleteUser(testUser.getId());

        Optional<User> deletedUser = userRepository.findById(testUser.getId());
        assertThat(deletedUser).isEmpty();
    }

    @Test
    void whenDeleteNonExistentUser_thenThrowException() {
        String nonExistentId = "507f1f77bcf86cd799439011";

        assertThatThrownBy(() -> userService.deleteUser(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void whenCheckIfUserExistsByEmail_thenReturnCorrectResult() {
        boolean exists = userRepository.existsByEmail(testUser.getEmail());
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void whenCheckIfUserExistsByUsername_thenReturnCorrectResult() {
        boolean exists = userRepository.existsByUsername(testUser.getUsername());
        boolean notExists = userRepository.existsByUsername("nonexistentuser");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
} 