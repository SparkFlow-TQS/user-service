package tqs.sparkflow.userservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.springframework.context.annotation.Profile;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@Profile("test")
public class TestcontainersConfiguration {

    private static final MongoDBContainer mongoDBContainer;

    static {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withReuse(true)
            .withExposedPorts(27017)
            .withStartupTimeout(java.time.Duration.ofSeconds(60))
            .withStartupAttempts(3);
        
        mongoDBContainer.start();
    }

    @Bean
    @ServiceConnection
    public MongoDBContainer mongoDBContainer() {
        return mongoDBContainer;
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getConnectionString);
        registry.add("spring.data.mongodb.database", () -> "test");
        registry.add("spring.data.mongodb.auto-index-creation", () -> true);
        registry.add("spring.data.mongodb.connect-timeout", () -> 30000);
        registry.add("spring.data.mongodb.socket-timeout", () -> 30000);
        registry.add("spring.data.mongodb.max-wait-time", () -> 30000);
        registry.add("spring.data.mongodb.server-selection-timeout", () -> 30000);
    }
} 