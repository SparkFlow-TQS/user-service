package tqs.sparkflow.userservice.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tqs.sparkflow.userservice.UserServiceApplication;
import tqs.sparkflow.userservice.TestcontainersConfiguration;
import tqs.sparkflow.userservice.config.TestConfig;

@CucumberContextConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        UserServiceApplication.class,
        TestConfig.class,
        TestcontainersConfiguration.class,
        CucumberTestConfig.class
    },
    properties = {"spring.main.allow-bean-definition-overriding=true"})
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
}
