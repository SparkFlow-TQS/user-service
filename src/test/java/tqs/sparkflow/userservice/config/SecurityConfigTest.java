package tqs.sparkflow.userservice.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import tqs.sparkflow.userservice.security.JwtAuthenticationFilter;
import tqs.sparkflow.userservice.security.CustomUserDetailsService;
import tqs.sparkflow.userservice.util.JwtUtil;

/**
 * Test class for SecurityConfig.
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private HttpSecurity httpSecurity;

    private SecurityConfig securityConfig;

    @Test
    void testPasswordEncoder() {
        // Given
        securityConfig = new SecurityConfig(jwtAuthenticationFilter);
        
        // When
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        // Then
        assertThat(encoder).isNotNull();
        
        String rawPassword = "testPassword123";
        String encodedPassword = encoder.encode(rawPassword);
        
        assertThat(encodedPassword)
            .isNotNull()
            .isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encodedPassword)).isTrue();
        assertThat(encoder.matches("wrongPassword", encodedPassword)).isFalse();
    }

    @Test 
    void testAuthenticationManager() throws Exception {
        // Given
        securityConfig = new SecurityConfig(jwtAuthenticationFilter);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        // When
        AuthenticationManager result = securityConfig.authenticationManager(authenticationConfiguration);

        // Then
        assertThat(result)
            .isNotNull()
            .isEqualTo(authenticationManager);
    }

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("test")
    static class SecurityIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CustomUserDetailsService userDetailsService;

        @MockBean
        private JwtUtil jwtUtil;

        @Test
        void testPublicEndpointsAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/auth/login"))
                   .andExpect(status().isMethodNotAllowed()); // POST expected, but accessible

            mockMvc.perform(get("/api/v1/auth/register"))
                   .andExpect(status().isMethodNotAllowed()); // POST expected, but accessible
        }

        @Test 
        void testProtectedEndpointsRequireAuth() throws Exception {
            mockMvc.perform(get("/api/v1/users"))
                   .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/v1/users/profile"))
                   .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        void testAuthenticatedAccess() throws Exception {
            mockMvc.perform(get("/api/v1/users/test"))
                   .andExpect(status().isOk());
        }

        @Test
        void testSecurityHeaders() throws Exception {
            mockMvc.perform(get("/api/v1/auth/login"))
                   .andExpect(header().exists("X-Content-Type-Options"))
                   .andExpect(header().exists("X-Frame-Options"))
                   .andExpect(header().exists("X-XSS-Protection"));
        }
    }
}