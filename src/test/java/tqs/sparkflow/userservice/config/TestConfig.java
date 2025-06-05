package tqs.sparkflow.userservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@Profile("test")
public class TestConfig extends SecurityConfig {

    @Bean
    @Primary
    @Override
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .httpBasic(httpBasic -> {});
        
        return http.build();
    }

    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        UserDetails testUser = User.builder()
            .username("test")
            .password(passwordEncoder().encode("test"))
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(testUser);
    }
} 