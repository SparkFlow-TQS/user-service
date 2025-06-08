package tqs.sparkflow.userservice.cucumber;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tqs.sparkflow.userservice.service.UserService;
import tqs.sparkflow.userservice.repository.UserRepository;

@TestConfiguration
@ActiveProfiles("test")
public class CucumberTestConfig {

  @Bean
  public String localServerPort(Environment environment) {
    return environment.getProperty("local.server.port", "0");
  }

  @Bean
  public TestRestTemplate testRestTemplate() {
    return new TestRestTemplate();
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }


  @Bean
  @Primary
  public UserService userService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    return new UserService(userRepository, passwordEncoder);
  }
}
