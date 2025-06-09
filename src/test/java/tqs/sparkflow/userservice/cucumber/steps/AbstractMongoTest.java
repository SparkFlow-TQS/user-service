package tqs.sparkflow.userservice.cucumber.steps;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Testcontainers
public abstract class AbstractMongoTest {

    @Container
    public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
        .withReuse(true)
        .withExposedPorts(27017)
        .withStartupTimeout(java.time.Duration.ofSeconds(60))
        .withStartupAttempts(3);

    static {
        mongoDBContainer.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "test");
        registry.add("spring.data.mongodb.auto-index-creation", () -> true);
        registry.add("spring.data.mongodb.connect-timeout", () -> 30000);
        registry.add("spring.data.mongodb.socket-timeout", () -> 30000);
        registry.add("spring.data.mongodb.max-wait-time", () -> 30000);
    }
}
