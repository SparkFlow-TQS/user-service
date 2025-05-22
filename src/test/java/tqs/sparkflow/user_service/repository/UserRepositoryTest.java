package tqs.sparkflow.user_service.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tqs.sparkflow.user_service.model.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers
class UserRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.2");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
    }

    @Test
    void whenSaveUser_thenUserIsSaved() {
        User savedUser = userRepository.save(testUser);
        
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(savedUser.getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    void whenFindByEmail_thenUserIsFound() {
        userRepository.save(testUser);
        
        Optional<User> found = userRepository.findByEmail("test@example.com");
        
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(testUser.getUsername());
    }

    @Test
    void whenFindByUsername_thenUserIsFound() {
        userRepository.save(testUser);
        
        Optional<User> found = userRepository.findByUsername("testuser");
        
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    void whenCheckExistsByEmail_thenReturnsTrue() {
        userRepository.save(testUser);
        
        boolean exists = userRepository.existsByEmail("test@example.com");
        
        assertThat(exists).isTrue();
    }

    @Test
    void whenCheckExistsByUsername_thenReturnsTrue() {
        userRepository.save(testUser);
        
        boolean exists = userRepository.existsByUsername("testuser");
        
        assertThat(exists).isTrue();
    }

    @Test
    void whenDeleteUser_thenUserIsRemoved() {
        User savedUser = userRepository.save(testUser);
        
        userRepository.deleteById(savedUser.getId());
        
        assertThat(userRepository.findById(savedUser.getId())).isEmpty();
    }
} 