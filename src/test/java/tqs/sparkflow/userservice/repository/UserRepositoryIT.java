package tqs.sparkflow.userservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import tqs.sparkflow.userservice.config.TestcontainersConfiguration;
import tqs.sparkflow.userservice.model.User;

@DataMongoTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Testcontainers
class UserRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User operatorUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        testUser = new User("testuser", "test@example.com", "password123");
        operatorUser = new User("operator", "operator@example.com", "password123", true);
        
        userRepository.saveAll(List.of(testUser, operatorUser));
    }

    @Test
    void whenFindByEmail_thenReturnUser() {
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void whenFindByNonExistentEmail_thenReturnEmpty() {
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        assertThat(foundUser).isEmpty();
    }

    @Test
    void whenFindByUsername_thenReturnUser() {
        Optional<User> foundUser = userRepository.findByUsername("testuser");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void whenFindByNonExistentUsername_thenReturnEmpty() {
        Optional<User> foundUser = userRepository.findByUsername("nonexistentuser");

        assertThat(foundUser).isEmpty();
    }

    @Test
    void whenExistsByEmail_thenReturnTrue() {
        boolean exists = userRepository.existsByEmail("test@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    void whenExistsByNonExistentEmail_thenReturnFalse() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    void whenExistsByUsername_thenReturnTrue() {
        boolean exists = userRepository.existsByUsername("testuser");

        assertThat(exists).isTrue();
    }

    @Test
    void whenExistsByNonExistentUsername_thenReturnFalse() {
        boolean exists = userRepository.existsByUsername("nonexistentuser");

        assertThat(exists).isFalse();
    }

    @Test
    void whenSaveUser_thenUserIsPersisted() {
        User newUser = new User("newuser", "newuser@example.com", "password123");
        
        User savedUser = userRepository.save(newUser);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedUser.isOperator()).isFalse();
    }

    @Test
    void whenSaveOperatorUser_thenOperatorFlagIsSet() {
        User newOperator = new User("newoperator", "newoperator@example.com", "password123", true);
        
        User savedUser = userRepository.save(newOperator);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("newoperator");
        assertThat(savedUser.getEmail()).isEqualTo("newoperator@example.com");
        assertThat(savedUser.isOperator()).isTrue();
    }

    @Test
    void whenFindAll_thenReturnAllUsers() {
        List<User> users = userRepository.findAll();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername)
                .containsExactlyInAnyOrder("testuser", "operator");
    }

    @Test
    void whenDeleteUser_thenUserIsRemoved() {
        userRepository.delete(testUser);

        Optional<User> deletedUser = userRepository.findById(testUser.getId());
        assertThat(deletedUser).isEmpty();
        
        List<User> remainingUsers = userRepository.findAll();
        assertThat(remainingUsers).hasSize(1);
        assertThat(remainingUsers.get(0).getUsername()).isEqualTo("operator");
    }

    @Test
    void whenDeleteById_thenUserIsRemoved() {
        String userId = testUser.getId();
        userRepository.deleteById(userId);

        Optional<User> deletedUser = userRepository.findById(userId);
        assertThat(deletedUser).isEmpty();
        
        List<User> remainingUsers = userRepository.findAll();
        assertThat(remainingUsers).hasSize(1);
        assertThat(remainingUsers.get(0).getUsername()).isEqualTo("operator");
    }

    @Test
    void whenDeleteAll_thenAllUsersAreRemoved() {
        userRepository.deleteAll();

        List<User> users = userRepository.findAll();
        assertThat(users).isEmpty();
    }

    @Test
    void whenFindById_thenReturnUser() {
        Optional<User> foundUser = userRepository.findById(testUser.getId());

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void whenFindByNonExistentId_thenReturnEmpty() {
        Optional<User> foundUser = userRepository.findById("507f1f77bcf86cd799439011");

        assertThat(foundUser).isEmpty();
    }

    @Test
    void whenExistsById_thenReturnTrue() {
        boolean exists = userRepository.existsById(testUser.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void whenExistsByNonExistentId_thenReturnFalse() {
        boolean exists = userRepository.existsById("507f1f77bcf86cd799439011");

        assertThat(exists).isFalse();
    }
} 