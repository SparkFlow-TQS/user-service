package tqs.sparkflow.userservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

import tqs.sparkflow.userservice.dto.JwtResponseDto;
import tqs.sparkflow.userservice.dto.LoginDto;
import tqs.sparkflow.userservice.dto.RefreshTokenRequestDto;
import tqs.sparkflow.userservice.dto.RegisterDto;
import tqs.sparkflow.userservice.exception.AuthenticationException;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.ValidationException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = {AuthController.class, AuthControllerTest.TestSecurityConfig.class, tqs.sparkflow.userservice.exception.GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private LoginDto loginDTO;
    private RegisterDto registerDTO;

    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth/**").permitAll()
                    .anyRequest().authenticated()
                );
            return http.build();
        }
    }

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("1");
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPassword("password123");

        loginDTO = new LoginDto();
        loginDTO.setEmailOrUsername("test@example.com");
        loginDTO.setPassword("password123");

        registerDTO = new RegisterDto();
        registerDTO.setEmail("test@example.com");
        registerDTO.setUsername("testuser");
        registerDTO.setPassword("password123");
    }

    @Test
    void whenLoginWithValidCredentials_thenReturnJwtResponse() throws Exception {
        JwtResponseDto jwtResponse = new JwtResponseDto("access-token", "refresh-token", 
            testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        when(authService.login(any(LoginDto.class))).thenReturn(jwtResponse);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.operator").value(testUser.isOperator()));
    }

    private static Stream<Arguments> exceptionHandlingTestCases() {
        return Stream.of(
            Arguments.of("/auth/login", new AuthenticationException("Invalid credentials"), HttpStatus.UNAUTHORIZED, "login"),
            Arguments.of("/auth/login", new ValidationException("Invalid input data"), HttpStatus.BAD_REQUEST, "login"),
            Arguments.of("/auth/register", new DuplicateEmailException("Email already exists"), HttpStatus.CONFLICT, "register"),
            Arguments.of("/auth/register", new ValidationException("Invalid input data"), HttpStatus.BAD_REQUEST, "register"),
            Arguments.of("/auth/refresh", new AuthenticationException("Invalid refresh token"), HttpStatus.UNAUTHORIZED, "refresh"),
            Arguments.of("/auth/refresh", new ValidationException("Refresh token is required"), HttpStatus.BAD_REQUEST, "refresh")
        );
    }

    @ParameterizedTest
    @MethodSource("exceptionHandlingTestCases")
    void whenServiceThrowsException_thenReturnExpectedHttpStatus(String endpoint, Exception exception, HttpStatus expectedStatus, String operation) throws Exception {
        // Given
        Object requestDto;
        switch (operation) {
            case "login":
                when(authService.login(any(LoginDto.class))).thenThrow(exception);
                requestDto = loginDTO;
                break;
            case "register":
                when(authService.register(any(RegisterDto.class))).thenThrow(exception);
                requestDto = registerDTO;
                break;
            case "refresh":
                when(authService.refreshToken(any(RefreshTokenRequestDto.class))).thenThrow(exception);
                RefreshTokenRequestDto refreshRequest = new RefreshTokenRequestDto();
                refreshRequest.setRefreshToken("test-token");
                requestDto = refreshRequest;
                break;
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }

        // When & Then
        mockMvc.perform(post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().is(expectedStatus.value()));
    }



    @Test
    void whenRegisterWithValidData_thenReturnCreatedUser() throws Exception {
        when(authService.register(any(RegisterDto.class))).thenReturn(testUser);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()));
    }


    @Test
    void whenRefreshTokenWithValidToken_thenReturnNewTokens() throws Exception {
        // Given
        RefreshTokenRequestDto refreshRequest = new RefreshTokenRequestDto();
        refreshRequest.setRefreshToken("valid-refresh-token");
        
        JwtResponseDto jwtResponse = new JwtResponseDto();
        jwtResponse.setAccessToken("new-access-token");
        jwtResponse.setRefreshToken("new-refresh-token");
        jwtResponse.setTokenType("Bearer");
        jwtResponse.setUsername("testuser");
        jwtResponse.setEmail("test@example.com");

        when(authService.refreshToken(any(RefreshTokenRequestDto.class))).thenReturn(jwtResponse);

        // When & Then
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
} 