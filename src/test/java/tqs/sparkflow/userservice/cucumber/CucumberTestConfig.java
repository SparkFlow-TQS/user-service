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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;

@TestConfiguration
@ActiveProfiles("test")
public class CucumberTestConfig {

  @Bean
  public String localServerPort(Environment environment) {
    return environment.getProperty("local.server.port", "0");
  }

  @Bean
  @Primary
  public TestRestTemplate testRestTemplate() {
    // Use Apache HttpClient instead of default Java HTTP client to avoid streaming mode issues
    RequestConfig config = RequestConfig.custom()
        .setConnectionRequestTimeout(Timeout.ofSeconds(30))
        .setResponseTimeout(Timeout.ofSeconds(30))
        .build();
    
    CloseableHttpClient httpClient = HttpClients.custom()
        .setDefaultRequestConfig(config)
        .build();
    
    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
    
    TestRestTemplate restTemplate = new TestRestTemplate();
    restTemplate.getRestTemplate().setRequestFactory(requestFactory);
    
    return restTemplate;
  }

  @Bean
  @Primary
  public RestTemplate restTemplate() {
    // Use Apache HttpClient for regular RestTemplate as well
    RequestConfig config = RequestConfig.custom()
        .setConnectionRequestTimeout(Timeout.ofSeconds(30))
        .setResponseTimeout(Timeout.ofSeconds(30))
        .build();
    
    CloseableHttpClient httpClient = HttpClients.custom()
        .setDefaultRequestConfig(config)
        .build();
    
    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
    
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setRequestFactory(requestFactory);
    
    return restTemplate;
  }

  @Bean
  @Primary
  public UserService userService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    return new UserService(userRepository, passwordEncoder);
  }
}
