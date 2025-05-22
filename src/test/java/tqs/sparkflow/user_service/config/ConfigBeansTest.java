package tqs.sparkflow.user_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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
