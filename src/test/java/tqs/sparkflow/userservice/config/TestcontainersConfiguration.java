package tqs.sparkflow.userservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;

@TestConfiguration
public class TestcontainersConfiguration {

    private static final MongoDBContainer mongoDBContainer;

    static {
        mongoDBContainer = new MongoDBContainer("mongo:6.0.2")
            .withReuse(true);
        mongoDBContainer.start();
    }

    @Bean
    @ServiceConnection
    public MongoDBContainer mongoDBContainer() {
        return mongoDBContainer;
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "test");
        registry.add("spring.data.mongodb.auto-index-creation", () -> true);
    }
} 