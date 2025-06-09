package tqs.sparkflow.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import tqs.sparkflow.userservice.dto.UserCreateDto;
import tqs.sparkflow.userservice.dto.UserUpdateDto;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.ResourceNotFoundException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserCreateDto createDTO;
    private UserUpdateDto updateDTO;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password123");
        testUser.setId("1");
        testUser.setOperator(false);

        createDTO = new UserCreateDto();
        createDTO.setUsername("newuser");
        createDTO.setEmail("new@example.com");
        createDTO.setPassword("password123");
        createDTO.setOperator(false);

        updateDTO = new UserUpdateDto();
        updateDTO.setUsername("updateduser");
        updateDTO.setEmail("updated@example.com");
        updateDTO.setPassword("newpassword");
        updateDTO.setOperator(true);
        
        // Setup passwordEncoder mock (lenient to avoid unnecessary stubbing errors)
        lenient().when(passwordEncoder.encode(any(String.class))).thenAnswer(i -> "encoded_" + i.getArgument(0));
    }

    @Test
    void whenCreateUser_thenReturnCreatedUser() {
        when(userRepository.existsByEmail(createDTO.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(createDTO.getUsername())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User createdUser = userService.createUser(createDTO);

        assertThat(createdUser.getUsername()).isEqualTo(createDTO.getUsername());
        assertThat(createdUser.getEmail()).isEqualTo(createDTO.getEmail());
        assertThat(createdUser.getPassword()).startsWith("encoded_"); // Password should be encoded
        assertThat(createdUser.isOperator()).isEqualTo(createDTO.isOperator());
    }

    @Test
    void whenCreateUserWithExistingEmail_thenThrowException() {
        when(userRepository.existsByEmail(createDTO.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createDTO))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void whenGetUserById_thenReturnUser() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        User foundUser = userService.getUserById(testUser.getId());

        assertThat(foundUser).isEqualTo(testUser);
    }

    @Test
    void whenGetUserByIdNotFound_thenThrowException() {
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void whenGetUserByEmail_thenReturnUser() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        User foundUser = userService.getUserByEmail(testUser.getEmail());

        assertThat(foundUser).isEqualTo(testUser);
    }

    @Test
    void whenGetUserByEmailNotFound_thenThrowException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("nonexistent@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void whenUpdateUser_thenReturnUpdatedUser() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(updateDTO.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(updateDTO.getUsername())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User updatedUser = userService.updateUser(testUser.getId(), updateDTO);

        assertThat(updatedUser.getUsername()).isEqualTo(updateDTO.getUsername());
        assertThat(updatedUser.getEmail()).isEqualTo(updateDTO.getEmail());
        assertThat(updatedUser.getPassword()).startsWith("encoded_"); // Password should be encoded
        assertThat(updatedUser.isOperator()).isEqualTo(updateDTO.getOperator());
    }

    @Test
    void whenUpdateUserWithExistingEmail_thenThrowException() {
        User anotherUser = new User("anotheruser", "another@example.com", "password123");
        anotherUser.setId("2");

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(updateDTO.getEmail())).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> userService.updateUser(testUser.getId(), updateDTO))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void whenUpdateUserNotFound_thenThrowException() {
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser("nonexistent", updateDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void whenDeleteUser_thenUserIsDeleted() {
        when(userRepository.existsById(testUser.getId())).thenReturn(true);
        doNothing().when(userRepository).deleteById(testUser.getId());

        userService.deleteUser(testUser.getId());

        verify(userRepository).deleteById(testUser.getId());
    }

    @Test
    void whenDeleteUserNotFound_thenThrowException() {
        when(userRepository.existsById("nonexistent")).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void whenGetAllUsers_thenReturnUserList() {
        List<User> users = List.of(testUser);
        when(userRepository.findAll()).thenReturn(users);

        List<User> foundUsers = userService.getAllUsers();

        assertThat(foundUsers).isEqualTo(users);
    }
} 