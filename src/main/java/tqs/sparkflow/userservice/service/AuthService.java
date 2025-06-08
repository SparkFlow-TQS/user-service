package tqs.sparkflow.userservice.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import tqs.sparkflow.userservice.dto.JwtResponseDTO;
import tqs.sparkflow.userservice.dto.LoginDTO;
import tqs.sparkflow.userservice.dto.RefreshTokenRequestDTO;
import tqs.sparkflow.userservice.dto.RegisterDTO;
import tqs.sparkflow.userservice.exception.AuthenticationException;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.DuplicateUsernameException;
import tqs.sparkflow.userservice.exception.ValidationException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.repository.UserRepository;
import tqs.sparkflow.userservice.util.JwtUtil;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";
    private static final String EMAIL_EXISTS = "Email already exists: %s";
    private static final String USERNAME_EXISTS = "Username already exists: %s";
    private static final String INVALID_EMAIL = "Invalid email format";
    private static final String INVALID_USERNAME = "Username must be between 3 and 50 characters";
    private static final String INVALID_PASSWORD = "Password must be at least 8 characters long";
    private static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public JwtResponseDTO login(LoginDTO loginDTO) {
        validateLoginInput(loginDTO);

        User user = userRepository.findByEmailOrUsername(loginDTO.getEmailOrUsername())
                .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }

        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getEmail(), user.isOperator());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        return new JwtResponseDTO(accessToken, refreshToken, user.getUsername(), user.getEmail(), user.isOperator());
    }

    public JwtResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();
        
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new AuthenticationException(INVALID_REFRESH_TOKEN);
        }

        String username = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS));

        if (!jwtUtil.validateToken(refreshToken, username)) {
            throw new AuthenticationException(INVALID_REFRESH_TOKEN);
        }

        String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getEmail(), user.isOperator());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        return new JwtResponseDTO(newAccessToken, newRefreshToken, user.getUsername(), user.getEmail(), user.isOperator());
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