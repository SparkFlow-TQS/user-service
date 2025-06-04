package tqs.sparkflow.userservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.ResourceNotFoundException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.repository.UserRepository;
import tqs.sparkflow.userservice.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password123");
        testUser.setId("1");
    }

    @Test
    void whenCreateUser_thenReturnCreatedUser() {
        when(userRepository.existsByEmail(testUser.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User createdUser = userService.createUser(testUser);

        assertNotNull(createdUser);
        assertEquals(testUser.getId(), createdUser.getId());
        assertEquals(testUser.getUsername(), createdUser.getUsername());
        assertEquals(testUser.getEmail(), createdUser.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void whenCreateUserWithExistingEmail_thenThrowException() {
        when(userRepository.existsByEmail(testUser.getEmail())).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.createUser(testUser));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void whenGetUserById_thenReturnUser() {
        when(userRepository.findById("1")).thenReturn(Optional.of(testUser));

        User foundUser = userService.getUserById("1");

        assertNotNull(foundUser);
        assertEquals(testUser.getId(), foundUser.getId());
    }

    @Test
    void whenGetUserByIdNotFound_thenThrowException() {
        when(userRepository.findById("1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById("1"));
    }

    @Test
    void whenGetUserByEmail_thenReturnUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        User foundUser = userService.getUserByEmail("test@example.com");

        assertNotNull(foundUser);
        assertEquals(testUser.getEmail(), foundUser.getEmail());
    }

    @Test
    void whenGetUserByEmailNotFound_thenThrowException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByEmail("nonexistent@example.com"));
    }

    @Test
    void whenUpdateUser_thenReturnUpdatedUser() {
        User updatedUser = new User("updateduser", "test@example.com", "newpassword");
        updatedUser.setId("1");

        when(userRepository.findById("1")).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        User result = userService.updateUser("1", updatedUser);

        assertNotNull(result);
        assertEquals(updatedUser.getUsername(), result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void whenUpdateUserWithNewEmail_thenReturnUpdatedUser() {
        User updatedUser = new User("updateduser", "new@example.com", "newpassword");
        updatedUser.setId("1");

        when(userRepository.findById("1")).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        User result = userService.updateUser("1", updatedUser);

        assertNotNull(result);
        assertEquals(updatedUser.getEmail(), result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void whenUpdateUserWithExistingEmail_thenThrowException() {
        User updatedUser = new User("updateduser", "existing@example.com", "newpassword");
        updatedUser.setId("1");
        User existingUser = new User("existinguser", "existing@example.com", "password123");
        existingUser.setId("2");

        when(userRepository.findById("1")).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        assertThrows(DuplicateEmailException.class, () -> userService.updateUser("1", updatedUser));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void whenDeleteUser_thenSuccess() {
        when(userRepository.existsById("1")).thenReturn(true);
        doNothing().when(userRepository).deleteById("1");

        userService.deleteUser("1");

        verify(userRepository).deleteById("1");
    }

    @Test
    void whenDeleteUserNotFound_thenThrowException() {
        when(userRepository.existsById("1")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser("1"));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void whenGetAllUsers_thenReturnUserList() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);

        List<User> foundUsers = userService.getAllUsers();

        assertNotNull(foundUsers);
        assertEquals(1, foundUsers.size());
        assertEquals(testUser.getId(), foundUsers.get(0).getId());
    }

    @Test
    void whenGetAllUsersEmpty_thenReturnEmptyList() {
        when(userRepository.findAll()).thenReturn(Arrays.asList());

        List<User> foundUsers = userService.getAllUsers();

        assertNotNull(foundUsers);
        assertTrue(foundUsers.isEmpty());
    }
} 