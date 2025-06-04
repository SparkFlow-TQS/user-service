package tqs.sparkflow.userservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import tqs.sparkflow.userservice.config.OpenApiConfig;
import tqs.sparkflow.userservice.config.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ConfigBeansTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.2");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    OpenApiConfig openApiConfig;

    @Autowired
    SecurityConfig securityConfig;

    @Test
    void openApiConfigBeanExists() {
        assertThat(openApiConfig).isNotNull();
    }

    @Test
    void securityConfigBeanExists() {
        assertThat(securityConfig).isNotNull();
    }
}
