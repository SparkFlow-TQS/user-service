package tqs.sparkflow.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import tqs.sparkflow.userservice.dto.JwtResponseDTO;
import tqs.sparkflow.userservice.dto.LoginDTO;
import tqs.sparkflow.userservice.dto.RegisterDTO;
import tqs.sparkflow.userservice.exception.AuthenticationException;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.DuplicateUsernameException;
import tqs.sparkflow.userservice.exception.ValidationException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.repository.UserRepository;
import tqs.sparkflow.userservice.util.JwtUtil;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginDTO loginDTO;
    private RegisterDTO registerDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("1");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setOperator(false);

        loginDTO = new LoginDTO();
        loginDTO.setEmailOrUsername("test@example.com");
        loginDTO.setPassword("password123");

        registerDTO = new RegisterDTO();
        registerDTO.setEmail("test@example.com");
        registerDTO.setUsername("testuser");
        registerDTO.setPassword("password123");
    }

    @Test
    @XrayTest(key = "AUTH-1")
    @Requirement("AUTH-1")
    void whenLoginWithValidCredentials_thenReturnJwtResponse() {
        when(userRepository.findByEmailOrUsername(loginDTO.getEmailOrUsername()))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginDTO.getPassword(), testUser.getPassword()))
            .thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString(), any(Boolean.class)))
            .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyString()))
            .thenReturn("refresh-token");

        JwtResponseDTO result = authService.login(loginDTO);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(result.isOperator()).isEqualTo(testUser.isOperator());
    }

    @Test
    @XrayTest(key = "AUTH-2")
    @Requirement("AUTH-2")
    void whenLoginWithInvalidEmail_thenThrowException() {
        when(userRepository.findByEmailOrUsername(loginDTO.getEmailOrUsername()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginDTO))
            .isInstanceOf(AuthenticationException.class)
            .hasMessage("Invalid credentials");
    }

    @Test
    @XrayTest(key = "AUTH-3")
    @Requirement("AUTH-3")
    void whenLoginWithInvalidPassword_thenThrowException() {
        when(userRepository.findByEmailOrUsername(loginDTO.getEmailOrUsername()))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginDTO.getPassword(), testUser.getPassword()))
            .thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginDTO))
            .isInstanceOf(AuthenticationException.class)
            .hasMessage("Invalid credentials");
    }

    @Test
    @XrayTest(key = "AUTH-4")
    @Requirement("AUTH-4")
    void whenRegisterWithValidData_thenReturnCreatedUser() {
        when(userRepository.existsByEmail(registerDTO.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerDTO.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(registerDTO.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = authService.register(registerDTO);

        assertThat(result.getUsername()).isEqualTo(registerDTO.getUsername());
        assertThat(result.getEmail()).isEqualTo(registerDTO.getEmail());
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        assertThat(result.isOperator()).isFalse();
    }

    @Test
    @XrayTest(key = "AUTH-5")
    @Requirement("AUTH-5")
    void whenRegisterWithExistingEmail_thenThrowException() {
        when(userRepository.existsByEmail(registerDTO.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerDTO))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessageContaining("Email already exists");
    }

    @Test
    @XrayTest(key = "AUTH-6")
    @Requirement("AUTH-6")
    void whenRegisterWithExistingUsername_thenThrowException() {
        when(userRepository.existsByEmail(registerDTO.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerDTO.getUsername())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerDTO))
            .isInstanceOf(DuplicateUsernameException.class)
            .hasMessageContaining("Username already exists");
    }

    @Test
    @XrayTest(key = "AUTH-7")
    @Requirement("AUTH-7")
    void whenLoginWithUsername_thenReturnJwtResponse() {
        loginDTO.setEmailOrUsername("testuser");
        when(userRepository.findByEmailOrUsername(loginDTO.getEmailOrUsername()))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginDTO.getPassword(), testUser.getPassword()))
            .thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString(), any(Boolean.class)))
            .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyString()))
            .thenReturn("refresh-token");

        JwtResponseDTO result = authService.login(loginDTO);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(result.isOperator()).isEqualTo(testUser.isOperator());
    }

    @Test
    @XrayTest(key = "AUTH-8")
    @Requirement("AUTH-8")
    void whenLoginWithEmptyCredentials_thenThrowValidationException() {
        loginDTO.setEmailOrUsername("");
        loginDTO.setPassword("");

        assertThatThrownBy(() -> authService.login(loginDTO))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Invalid credentials");
    }

    @Test
    @XrayTest(key = "AUTH-9")
    @Requirement("AUTH-9")
    void whenRegisterWithInvalidEmail_thenThrowValidationException() {
        registerDTO.setEmail("invalid-email");

        assertThatThrownBy(() -> authService.register(registerDTO))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Invalid email format");
    }

    @Test
    @XrayTest(key = "AUTH-10")
    @Requirement("AUTH-10")
    void whenRegisterWithInvalidUsername_thenThrowValidationException() {
        registerDTO.setUsername("ab"); // Too short

        assertThatThrownBy(() -> authService.register(registerDTO))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Username must be between 3 and 50 characters");
    }

    @Test
    @XrayTest(key = "AUTH-11")
    @Requirement("AUTH-11")
    void whenRegisterWithInvalidPassword_thenThrowValidationException() {
        registerDTO.setPassword("123"); // Too short

        assertThatThrownBy(() -> authService.register(registerDTO))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Password must be at least 8 characters long");
    }
} 