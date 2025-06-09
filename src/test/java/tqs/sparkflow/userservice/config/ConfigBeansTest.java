package tqs.sparkflow.userservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tqs.sparkflow.userservice.UserServiceApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
    classes = {
        UserServiceApplication.class,
        TestConfig.class,
        TestcontainersConfiguration.class
    }
)
@ActiveProfiles("test")
class ConfigBeansTest {

    @Autowired
    private OpenApiConfig openApiConfig;

    @Autowired
    private SecurityConfig securityConfig;

    @Test
    void testOpenApiConfigBean() {
        assertNotNull(openApiConfig);
    }

    @Test
    void testSecurityConfigBean() {
        assertNotNull(securityConfig);
    }
}
