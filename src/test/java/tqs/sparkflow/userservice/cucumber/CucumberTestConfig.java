package tqs.sparkflow.userservice.cucumber;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import tqs.sparkflow.userservice.service.UserService;
import tqs.sparkflow.userservice.repository.UserRepository;

@TestConfiguration
@EnableWebSecurity
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
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .headers(headers -> headers.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }

  @Bean
  @Primary
  public UserService userService(UserRepository userRepository) {
    return new UserService(userRepository);
  }
}
