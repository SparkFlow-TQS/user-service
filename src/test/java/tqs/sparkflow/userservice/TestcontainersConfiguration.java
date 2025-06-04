package tqs.sparkflow.userservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;

@TestConfiguration
public class TestcontainersConfiguration {

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry, MongoDBContainer mongoDBContainer) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "test");
        registry.add("spring.data.mongodb.auto-index-creation", () -> true);
    }
} 