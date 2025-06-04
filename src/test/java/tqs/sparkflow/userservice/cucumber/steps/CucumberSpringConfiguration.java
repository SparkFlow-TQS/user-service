package tqs.sparkflow.userservice.cucumber.steps;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tqs.sparkflow.userservice.config.TestConfig;
import tqs.sparkflow.userservice.config.WebConfig;
import tqs.sparkflow.userservice.UserServiceApplication;
import tqs.sparkflow.userservice.cucumber.CucumberTestConfig;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import tqs.sparkflow.userservice.cucumber.steps.AbstractMongoTest;

@CucumberContextConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        UserServiceApplication.class,
        TestConfig.class,
        WebConfig.class,
        CucumberTestConfig.class
    },
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.data.mongodb.auto-index-creation=true"
    }
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Testcontainers
public class CucumberSpringConfiguration extends AbstractMongoTest {
} 