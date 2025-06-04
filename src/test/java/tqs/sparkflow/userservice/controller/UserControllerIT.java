package tqs.sparkflow.userservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

import tqs.sparkflow.userservice.UserServiceApplication;
import tqs.sparkflow.userservice.TestcontainersConfiguration;
import tqs.sparkflow.userservice.config.TestConfig;
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
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = new User("testuser", "test@example.com", "password123");
        testUser = userRepository.save(testUser);
    }

    @Test
    void whenCreateUser_thenReturnCreatedUser() throws Exception {
        User newUser = new User("newuser", "new@example.com", "password123");

        MvcResult result = mockMvc.perform(post("/api/v1/users")
                .with(httpBasic("test", "test"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andReturn();

        User createdUser = objectMapper.readValue(result.getResponse().getContentAsString(), User.class);
        assertThat(createdUser.getUsername()).isEqualTo(newUser.getUsername());
        assertThat(createdUser.getEmail()).isEqualTo(newUser.getEmail());
    }

    @Test
    void whenCreateUserWithExistingEmail_thenReturnConflict() throws Exception {
        User duplicateUser = new User("anotheruser", testUser.getEmail(), "password123");

        mockMvc.perform(post("/api/v1/users")
                .with(httpBasic("test", "test"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isConflict());
    }

    @Test
    void whenCreateUserWithInvalidData_thenReturnBadRequest() throws Exception {
        User invalidUser = new User("", "invalid-email", "123"); // Invalid username, email, and password

        mockMvc.perform(post("/api/v1/users")
                .with(httpBasic("test", "test"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenGetUserById_thenReturnUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", testUser.getId())
                .with(httpBasic("test", "test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    void whenGetUserByEmail_thenReturnUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/email/{email}", testUser.getEmail())
                .with(httpBasic("test", "test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    void whenGetUserByIdNotFound_thenReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", "nonexistent")
                .with(httpBasic("test", "test")))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenGetUserByEmailNotFound_thenReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/email/{email}", "nonexistent@example.com")
                .with(httpBasic("test", "test")))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenUpdateUser_thenReturnUpdatedUser() throws Exception {
        User updatedUser = new User("updateduser", testUser.getEmail(), "newpassword");
        updatedUser.setId(testUser.getId());

        MvcResult result = mockMvc.perform(put("/api/v1/users/{id}", testUser.getId())
                .with(httpBasic("test", "test"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andReturn();

        User returnedUser = objectMapper.readValue(result.getResponse().getContentAsString(), User.class);
        assertThat(returnedUser.getUsername()).isEqualTo(updatedUser.getUsername());
        assertThat(returnedUser.getEmail()).isEqualTo(updatedUser.getEmail());
    }

    @Test
    void whenUpdateUserWithNewEmail_thenReturnUpdatedUser() throws Exception {
        User updatedUser = new User("updateduser", "new@example.com", "newpassword");
        updatedUser.setId(testUser.getId());

        MvcResult result = mockMvc.perform(put("/api/v1/users/{id}", testUser.getId())
                .with(httpBasic("test", "test"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andReturn();

        User returnedUser = objectMapper.readValue(result.getResponse().getContentAsString(), User.class);
        assertThat(returnedUser.getEmail()).isEqualTo(updatedUser.getEmail());
    }

    @Test
    void whenUpdateUserWithExistingEmail_thenReturnConflict() throws Exception {
        // Create another user first
        User anotherUser = new User("anotheruser", "another@example.com", "password123");
        anotherUser = userRepository.save(anotherUser);

        // Try to update testUser with anotherUser's email
        User updatedUser = new User("updateduser", anotherUser.getEmail(), "newpassword");
        updatedUser.setId(testUser.getId());

        mockMvc.perform(put("/api/v1/users/{id}", testUser.getId())
                .with(httpBasic("test", "test"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isConflict());
    }

    @Test
    void whenUpdateUserWithInvalidData_thenReturnBadRequest() throws Exception {
        User invalidUser = new User("", "invalid-email", "123");
        invalidUser.setId(testUser.getId());

        mockMvc.perform(put("/api/v1/users/{id}", testUser.getId())
                .with(httpBasic("test", "test"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenUpdateUserNotFound_thenReturnNotFound() throws Exception {
        User updatedUser = new User("updateduser", "new@example.com", "newpassword");
        updatedUser.setId("nonexistent");

        mockMvc.perform(put("/api/v1/users/{id}", "nonexistent")
                .with(httpBasic("test", "test"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenDeleteUser_thenReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", testUser.getId())
                .with(httpBasic("test", "test")))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(testUser.getId())).isEmpty();
    }

    @Test
    void whenDeleteUserNotFound_thenReturnNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", "nonexistent")
                .with(httpBasic("test", "test")))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenGetAllUsers_thenReturnUserList() throws Exception {
        // Create another user
        User anotherUser = new User("anotheruser", "another@example.com", "password123");
        userRepository.save(anotherUser);

        mockMvc.perform(get("/api/v1/users")
                .with(httpBasic("test", "test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(testUser.getId()))
                .andExpect(jsonPath("$[1].id").value(anotherUser.getId()));
    }

    @Test
    void whenGetAllUsersEmpty_thenReturnEmptyList() throws Exception {
        userRepository.deleteAll();

        mockMvc.perform(get("/api/v1/users")
                .with(httpBasic("test", "test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
} 