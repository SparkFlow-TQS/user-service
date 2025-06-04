package tqs.sparkflow.userservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tqs.sparkflow.userservice.model.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class UserRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User operator;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        user1 = new User("user1", "user1@example.com", "password123");
        user2 = new User("user2", "user2@example.com", "password123");
        operator = new User("operator", "operator@example.com", "password123", true);
        
        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(operator);
    }

    @Test
    void whenFindByEmail_thenReturnUser() {
        Optional<User> found = userRepository.findByEmail("user1@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("user1");
    }

    @Test
    void whenFindByUsername_thenReturnUser() {
        Optional<User> found = userRepository.findByUsername("user1");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("user1@example.com");
    }

    @Test
    void whenFindByEmailOrUsername_thenReturnUser() {
        Optional<User> foundByEmail = userRepository.findByEmailOrUsername("user1@example.com");
        Optional<User> foundByUsername = userRepository.findByEmailOrUsername("user1");
        
        assertThat(foundByEmail).isPresent();
        assertThat(foundByUsername).isPresent();
        assertThat(foundByEmail.get().getId()).isEqualTo(foundByUsername.get().getId());
    }

    @Test
    void whenFindByIsOperatorTrue_thenReturnOnlyOperators() {
        List<User> operators = userRepository.findByIsOperatorTrue();
        assertThat(operators).hasSize(1);
        assertThat(operators.get(0).getUsername()).isEqualTo("operator");
    }

    @Test
    void whenFindByIsOperatorFalse_thenReturnOnlyNonOperators() {
        List<User> nonOperators = userRepository.findByIsOperatorFalse();
        assertThat(nonOperators).hasSize(2);
        assertThat(nonOperators).extracting(User::getUsername)
            .containsExactlyInAnyOrder("user1", "user2");
    }

    @Test
    void whenFindByUsernameContainingIgnoreCase_thenReturnMatchingUsers() {
        List<User> users = userRepository.findByUsernameContainingIgnoreCase("user");
        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername)
            .containsExactlyInAnyOrder("user1", "user2");
    }

    @Test
    void whenFindByEmailContainingIgnoreCase_thenReturnMatchingUsers() {
        List<User> users = userRepository.findByEmailContainingIgnoreCase("example.com");
        assertThat(users).hasSize(3);
        assertThat(users).extracting(User::getEmail)
            .containsExactlyInAnyOrder(
                "user1@example.com",
                "user2@example.com",
                "operator@example.com"
            );
    }

    @Test
    void whenExistsByEmail_thenReturnTrue() {
        assertThat(userRepository.existsByEmail("user1@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    void whenExistsByUsername_thenReturnTrue() {
        assertThat(userRepository.existsByUsername("user1")).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }
} 