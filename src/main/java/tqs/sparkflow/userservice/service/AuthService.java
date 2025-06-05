package tqs.sparkflow.userservice.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import tqs.sparkflow.userservice.dto.LoginDTO;
import tqs.sparkflow.userservice.dto.RegisterDTO;
import tqs.sparkflow.userservice.exception.AuthenticationException;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.DuplicateUsernameException;
import tqs.sparkflow.userservice.exception.ValidationException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.repository.UserRepository;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";
    private static final String EMAIL_EXISTS = "Email already exists: %s";
    private static final String USERNAME_EXISTS = "Username already exists: %s";
    private static final String INVALID_EMAIL = "Invalid email format";
    private static final String INVALID_USERNAME = "Username must be between 3 and 50 characters";
    private static final String INVALID_PASSWORD = "Password must be at least 8 characters long";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User login(LoginDTO loginDTO) {
        validateLoginInput(loginDTO);

        User user = userRepository.findByEmailOrUsername(loginDTO.getEmailOrUsername())
                .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }

        return user;
    }

    public User register(RegisterDTO registerDTO) {
        validateRegistrationInput(registerDTO);
        checkForExistingUser(registerDTO);

        User user = createUserFromDTO(registerDTO);
        return userRepository.save(user);
    }

    private void validateLoginInput(LoginDTO loginDTO) {
        if (!StringUtils.hasText(loginDTO.getEmailOrUsername()) || !StringUtils.hasText(loginDTO.getPassword())) {
            throw new ValidationException(INVALID_CREDENTIALS);
        }
    }

    private void validateRegistrationInput(RegisterDTO registerDTO) {
        if (!StringUtils.hasText(registerDTO.getEmail()) || !isValidEmail(registerDTO.getEmail())) {
            throw new ValidationException(INVALID_EMAIL);
        }

        if (!StringUtils.hasText(registerDTO.getUsername()) || !isValidUsername(registerDTO.getUsername())) {
            throw new ValidationException(INVALID_USERNAME);
        }

        if (!StringUtils.hasText(registerDTO.getPassword()) || !isValidPassword(registerDTO.getPassword())) {
            throw new ValidationException(INVALID_PASSWORD);
        }
    }

    private void checkForExistingUser(RegisterDTO registerDTO) {
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new DuplicateEmailException(String.format(EMAIL_EXISTS, registerDTO.getEmail()));
        }

        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new DuplicateUsernameException(String.format(USERNAME_EXISTS, registerDTO.getUsername()));
        }
    }

    private User createUserFromDTO(RegisterDTO registerDTO) {
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setOperator(false);
        return user;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidUsername(String username) {
        return username.length() >= 3 && username.length() <= 50;
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 8;
    }
} 