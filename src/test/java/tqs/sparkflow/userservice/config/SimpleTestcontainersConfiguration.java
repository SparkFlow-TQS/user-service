package tqs.sparkflow.userservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@Profile("test")
public class SimpleTestcontainersConfiguration {

    private static final GenericContainer<?> mongoContainer;

    static {
        mongoContainer = new GenericContainer<>(DockerImageName.parse("mongo:5.0"))
            .withExposedPorts(27017)
            .withEnv("MONGO_INITDB_DATABASE", "test")
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(java.time.Duration.ofSeconds(120));
        
        mongoContainer.start();
    }

    @Bean
    public GenericContainer<?> mongoContainer() {
        return mongoContainer;
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.host", mongoContainer::getHost);
        registry.add("spring.data.mongodb.port", () -> mongoContainer.getMappedPort(27017));
        registry.add("spring.data.mongodb.database", () -> "test");
        registry.add("spring.data.mongodb.auto-index-creation", () -> true);
    }
}