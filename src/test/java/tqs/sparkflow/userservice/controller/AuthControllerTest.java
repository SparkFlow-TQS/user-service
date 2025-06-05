package tqs.sparkflow.userservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import tqs.sparkflow.userservice.config.TestConfig;
import tqs.sparkflow.userservice.config.WebConfig;
import tqs.sparkflow.userservice.dto.LoginDTO;
import tqs.sparkflow.userservice.dto.RegisterDTO;
import tqs.sparkflow.userservice.exception.AuthenticationException;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.service.AuthService;

@WebMvcTest(AuthController.class)
@Import({TestConfig.class, WebConfig.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginDTO loginDTO;
    private RegisterDTO registerDTO;
    private User testUser;

    @BeforeEach
    void setUp() {
        loginDTO = new LoginDTO();
        loginDTO.setEmailOrUsername("test@example.com");
        loginDTO.setPassword("password123");

        registerDTO = new RegisterDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setEmail("test@example.com");
        registerDTO.setPassword("password123");

        testUser = new User("testuser", "test@example.com", "password123");
        testUser.setId("1");
    }

    @Test
    void whenLoginWithValidCredentials_thenReturnUser() throws Exception {
        when(authService.login(any(LoginDTO.class))).thenReturn(testUser);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    void whenLoginWithInvalidCredentials_thenReturnUnauthorized() throws Exception {
        when(authService.login(any(LoginDTO.class)))
                .thenThrow(new AuthenticationException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenLoginWithInvalidData_thenReturnBadRequest() throws Exception {
        loginDTO.setEmailOrUsername(""); // Invalid email/username
        loginDTO.setPassword(""); // Invalid password

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenRegisterWithValidData_thenReturnCreatedUser() throws Exception {
        when(authService.register(any(RegisterDTO.class))).thenReturn(testUser);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    void whenRegisterWithExistingEmail_thenReturnConflict() throws Exception {
        when(authService.register(any(RegisterDTO.class)))
                .thenThrow(new DuplicateEmailException("Email already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    void whenRegisterWithInvalidData_thenReturnBadRequest() throws Exception {
        registerDTO.setUsername(""); // Invalid username
        registerDTO.setEmail("invalid-email"); // Invalid email
        registerDTO.setPassword("123"); // Invalid password

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isBadRequest());
    }
} 