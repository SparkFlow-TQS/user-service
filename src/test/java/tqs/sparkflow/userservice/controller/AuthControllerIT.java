package tqs.sparkflow.userservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

import tqs.sparkflow.userservice.UserServiceApplication;
import tqs.sparkflow.userservice.config.TestConfig;
import tqs.sparkflow.userservice.config.TestcontainersConfiguration;
import tqs.sparkflow.userservice.dto.LoginDTO;
import tqs.sparkflow.userservice.dto.RegisterDTO;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.repository.UserRepository;

@SpringBootTest(
    classes = {
        UserServiceApplication.class,
        TestConfig.class,
        TestcontainersConfiguration.class
    },
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.data.mongodb.auto-index-creation=true"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private LoginDTO loginDTO;
    private RegisterDTO registerDTO;
    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = new User("testuser", "test@example.com", passwordEncoder.encode("password123"));
        testUser = userRepository.save(testUser);

        loginDTO = new LoginDTO();
        loginDTO.setEmailOrUsername("test@example.com");
        loginDTO.setPassword("password123");

        registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setEmail("new@example.com");
        registerDTO.setPassword("password123");
    }

    @Test
    void whenLoginWithValidCredentials_thenReturnUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andReturn();

        User returnedUser = objectMapper.readValue(result.getResponse().getContentAsString(), User.class);
        assertThat(returnedUser.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(returnedUser.getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    void whenLoginWithInvalidCredentials_thenReturnUnauthorized() throws Exception {
        loginDTO.setPassword("wrongpassword");

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
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        User createdUser = objectMapper.readValue(result.getResponse().getContentAsString(), User.class);
        assertThat(createdUser.getUsername()).isEqualTo(registerDTO.getUsername());
        assertThat(createdUser.getEmail()).isEqualTo(registerDTO.getEmail());
    }

    @Test
    void whenRegisterWithExistingEmail_thenReturnConflict() throws Exception {
        registerDTO.setEmail(testUser.getEmail()); // Use existing email

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    void whenRegisterWithExistingUsername_thenReturnConflict() throws Exception {
        registerDTO.setUsername(testUser.getUsername()); // Use existing username

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