package tqs.sparkflow.userservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;

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
import tqs.sparkflow.userservice.dto.UserCreateDTO;
import tqs.sparkflow.userservice.dto.UserUpdateDTO;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.ResourceNotFoundException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.service.UserService;

@WebMvcTest(UserController.class)
@Import({TestConfig.class, WebConfig.class})
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private UserCreateDTO createDTO;
    private UserUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password123");
        testUser.setId("1");
        testUser.setOperator(false);

        createDTO = new UserCreateDTO();
        createDTO.setUsername("newuser");
        createDTO.setEmail("new@example.com");
        createDTO.setPassword("password123");
        createDTO.setOperator(false);

        updateDTO = new UserUpdateDTO();
        updateDTO.setUsername("updateduser");
        updateDTO.setEmail("updated@example.com");
        updateDTO.setPassword("newpassword");
        updateDTO.setOperator(true);
    }

    @Test
    void whenCreateUser_thenReturnCreatedUser() throws Exception {
        when(userService.createUser(any(UserCreateDTO.class))).thenReturn(testUser);

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    void whenCreateUserWithExistingEmail_thenReturnConflict() throws Exception {
        when(userService.createUser(any(UserCreateDTO.class)))
                .thenThrow(new DuplicateEmailException("Email already exists"));

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    void whenCreateUserWithInvalidData_thenReturnBadRequest() throws Exception {
        createDTO.setUsername(""); // Invalid username
        createDTO.setEmail("invalid-email"); // Invalid email
        createDTO.setPassword("123"); // Invalid password

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenGetUserById_thenReturnUser() throws Exception {
        when(userService.getUserById(testUser.getId())).thenReturn(testUser);

        mockMvc.perform(get("/api/v1/users/{id}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    void whenGetUserByIdNotFound_thenReturnNotFound() throws Exception {
        when(userService.getUserById("nonexistent"))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/users/{id}", "nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenGetUserByEmail_thenReturnUser() throws Exception {
        when(userService.getUserByEmail(testUser.getEmail())).thenReturn(testUser);

        mockMvc.perform(get("/api/v1/users/email/{email}", testUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    void whenGetUserByEmailNotFound_thenReturnNotFound() throws Exception {
        when(userService.getUserByEmail("nonexistent@example.com"))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/users/email/{email}", "nonexistent@example.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenUpdateUser_thenReturnUpdatedUser() throws Exception {
        User updatedUser = new User(updateDTO.getUsername(), updateDTO.getEmail(), updateDTO.getPassword());
        updatedUser.setId(testUser.getId());
        updatedUser.setOperator(updateDTO.isOperator());

        when(userService.updateUser(eq(testUser.getId()), any(UserUpdateDTO.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/api/v1/users/{id}", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(updateDTO.getUsername()))
                .andExpect(jsonPath("$.email").value(updateDTO.getEmail()));
    }

    @Test
    void whenUpdateUserWithExistingEmail_thenReturnConflict() throws Exception {
        when(userService.updateUser(eq(testUser.getId()), any(UserUpdateDTO.class)))
                .thenThrow(new DuplicateEmailException("Email already exists"));

        mockMvc.perform(put("/api/v1/users/{id}", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    void whenUpdateUserWithInvalidData_thenReturnBadRequest() throws Exception {
        updateDTO.setUsername(""); // Invalid username
        updateDTO.setEmail("invalid-email"); // Invalid email
        updateDTO.setPassword("123"); // Invalid password

        mockMvc.perform(put("/api/v1/users/{id}", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenUpdateUserNotFound_thenReturnNotFound() throws Exception {
        when(userService.updateUser(eq("nonexistent"), any(UserUpdateDTO.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/api/v1/users/{id}", "nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenDeleteUser_thenReturnNoContent() throws Exception {
        doNothing().when(userService).deleteUser(testUser.getId());

        mockMvc.perform(delete("/api/v1/users/{id}", testUser.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void whenDeleteUserNotFound_thenReturnNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found"))
            .when(userService).deleteUser("nonexistent");

        mockMvc.perform(delete("/api/v1/users/{id}", "nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenGetAllUsers_thenReturnUserList() throws Exception {
        List<User> users = Arrays.asList(testUser);
        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(testUser.getId()))
                .andExpect(jsonPath("$[0].username").value(testUser.getUsername()))
                .andExpect(jsonPath("$[0].email").value(testUser.getEmail()));
    }
} 