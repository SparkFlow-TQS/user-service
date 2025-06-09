package tqs.sparkflow.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import tqs.sparkflow.userservice.dto.UserCreateDto;
import tqs.sparkflow.userservice.dto.UserUpdateDto;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.ResourceNotFoundException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {UserController.class, UserControllerTest.TestSecurityConfig.class, tqs.sparkflow.userservice.exception.GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private UserCreateDto createDto;
    private UserUpdateDto updateDto;

    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/users/profile", "/users/test").authenticated()
                    .anyRequest().permitAll()
                )
                .httpBasic(httpBasic -> {});
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
        testUser.setOperator(false);

        createDto = new UserCreateDto();
        createDto.setUsername("newuser");
        createDto.setEmail("new@example.com");
        createDto.setPassword("password123");
        createDto.setOperator(false);

        updateDto = new UserUpdateDto();
        updateDto.setUsername("updateduser");
        updateDto.setEmail("updated@example.com");
        updateDto.setPassword("newpassword");
        updateDto.setOperator(true);
    }

    @Test
    void shouldReturnCreatedWhenValidUserDto() throws Exception {
        when(userService.createUser(any(UserCreateDto.class))).thenReturn(testUser);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.operator").value(false));

        verify(userService, times(1)).createUser(any(UserCreateDto.class));
    }

    @Test
    void shouldReturnConflictWhenDuplicateEmailException() throws Exception {
        when(userService.createUser(any(UserCreateDto.class)))
                .thenThrow(new DuplicateEmailException("Email already exists: new@example.com"));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isConflict());

        verify(userService, times(1)).createUser(any(UserCreateDto.class));
    }

    @Test
    void shouldReturnBadRequestWhenValidationFails() throws Exception {
        createDto.setUsername(""); // Invalid username
        createDto.setEmail("invalid-email"); // Invalid email format

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUserWhenValidId() throws Exception {
        when(userService.getUserById("1")).thenReturn(testUser);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService, times(1)).getUserById("1");
    }

    @Test
    void shouldReturnNotFoundWhenUserNotExists() throws Exception {
        when(userService.getUserById("nonexistent"))
                .thenThrow(new ResourceNotFoundException("User not found with id: nonexistent"));

        mockMvc.perform(get("/users/nonexistent"))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).getUserById("nonexistent");
    }

    @Test
    void shouldReturnUserWhenValidEmail() throws Exception {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);

        mockMvc.perform(get("/users/email/test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService, times(1)).getUserByEmail("test@example.com");
    }

    @Test
    void shouldReturnNotFoundWhenEmailNotExists() throws Exception {
        when(userService.getUserByEmail("nonexistent@example.com"))
                .thenThrow(new ResourceNotFoundException("User not found with email: nonexistent@example.com"));

        mockMvc.perform(get("/users/email/nonexistent@example.com"))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).getUserByEmail("nonexistent@example.com");
    }

    @Test
    void shouldReturnUpdatedUserWhenValidData() throws Exception {
        User updatedUser = new User();
        updatedUser.setId("1");
        updatedUser.setUsername("updateduser");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setOperator(true);

        when(userService.updateUser(eq("1"), any(UserUpdateDto.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.username").value("updateduser"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.operator").value(true));

        verify(userService, times(1)).updateUser(eq("1"), any(UserUpdateDto.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdateUserNotExists() throws Exception {
        when(userService.updateUser(eq("nonexistent"), any(UserUpdateDto.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: nonexistent"));

        mockMvc.perform(put("/users/nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).updateUser(eq("nonexistent"), any(UserUpdateDto.class));
    }

    @Test
    void shouldReturnConflictWhenUpdateDuplicateEmail() throws Exception {
        when(userService.updateUser(eq("1"), any(UserUpdateDto.class)))
                .thenThrow(new DuplicateEmailException("Email already exists: updated@example.com"));

        mockMvc.perform(put("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isConflict());

        verify(userService, times(1)).updateUser(eq("1"), any(UserUpdateDto.class));
    }

    @Test
    void shouldReturnNoContentWhenValidDelete() throws Exception {
        doNothing().when(userService).deleteUser("1");

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser("1");
    }

    @Test
    void shouldReturnNotFoundWhenDeleteUserNotExists() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: nonexistent"))
                .when(userService).deleteUser("nonexistent");

        mockMvc.perform(delete("/users/nonexistent"))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).deleteUser("nonexistent");
    }

    @Test
    void shouldReturnUserListWhenGetAllUsers() throws Exception {
        List<User> users = List.of(testUser);
        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].email").value("test@example.com"));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    void shouldReturnProfileWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.authorities[0].authority").value("ROLE_USER"));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_OPERATOR"})
    void shouldReturnTestMessageWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/users/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.user").value("testuser"));
    }

    @Test
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isUnauthorized());
    }
}