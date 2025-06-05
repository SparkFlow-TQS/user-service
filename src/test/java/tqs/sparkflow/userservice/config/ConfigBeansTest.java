package tqs.sparkflow.userservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import tqs.sparkflow.userservice.UserServiceApplication;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    classes = {
        UserServiceApplication.class,
        TestConfig.class,
        TestcontainersConfiguration.class
    }
)  
class ConfigBeansTest {

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
