package tqs.sparkflow.userservice.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tqs.sparkflow.userservice.dto.JwtResponseDto;
import tqs.sparkflow.userservice.dto.LoginDto;
import tqs.sparkflow.userservice.dto.RefreshTokenRequestDto;
import tqs.sparkflow.userservice.dto.RegisterDto;
import tqs.sparkflow.userservice.exception.AuthenticationException;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.DuplicateUsernameException;
import tqs.sparkflow.userservice.exception.ValidationException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.repository.UserRepository;
import tqs.sparkflow.userservice.util.JwtUtil;

/**
 * Service for authentication operations including login, registration, and token refresh.
 */
@Service
public class AuthService {

  private static final String INVALID_CREDENTIALS = "Invalid credentials";
  private static final String EMAIL_EXISTS = "Email already exists: %s";
  private static final int MIN_USERNAME_LENGTH = 3;
  private static final int MAX_USERNAME_LENGTH = 50;
  private static final int MIN_PASSWORD_LENGTH = 8;
  private static final String USERNAME_EXISTS = "Username already exists: %s";
  private static final String INVALID_EMAIL = "Invalid email format";
  private static final String INVALID_USERNAME = "Username must be between 3 and 50 characters";
  private static final String INVALID_PASSWORD = "Password must be at least 8 characters long";
  private static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  /**
   * Constructor for AuthService.
   *
   * @param userRepository the user repository
   * @param passwordEncoder the password encoder
   * @param jwtUtil the JWT utility
   */
  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                     JwtUtil jwtUtil) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
  }

  /**
   * Authenticates a user and returns JWT tokens.
   *
   * @param loginDto the login credentials
   * @return JWT response with tokens and user information
   * @throws AuthenticationException if credentials are invalid
   * @throws ValidationException if input data is invalid
   */
  public JwtResponseDto login(LoginDto loginDto) {
    validateLoginInput(loginDto);

    User user = userRepository.findByEmailOrUsername(loginDto.getEmailOrUsername())
        .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS));

    if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
      throw new AuthenticationException(INVALID_CREDENTIALS);
    }

    String accessToken = jwtUtil.generateToken(user.getUsername(), user.getEmail(), 
        user.isOperator());
    String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

    return new JwtResponseDto(accessToken, refreshToken, user.getUsername(), 
        user.getEmail(), user.isOperator());
  }

  private void validateLoginInput(LoginDto loginDto) {
    if (!StringUtils.hasText(loginDto.getEmailOrUsername()) 
        || !StringUtils.hasText(loginDto.getPassword())) {
      throw new ValidationException(INVALID_CREDENTIALS);
    }
  }

  /**
   * Registers a new user.
   *
   * @param registerDto the registration data
   * @return the registered user
   * @throws ValidationException if input data is invalid
   * @throws DuplicateEmailException if email already exists
   * @throws DuplicateUsernameException if username already exists
   */
  public User register(RegisterDto registerDto) {
    validateRegistrationInput(registerDto);
    checkForExistingUser(registerDto);
    User user = createUserFromDto(registerDto);
    return userRepository.save(user);
  }

  /**
   * Refreshes the access token using a refresh token.
   *
   * @param refreshTokenRequestDto the refresh token request
   * @return JWT response with new tokens
   * @throws AuthenticationException if refresh token is invalid
   */
  public JwtResponseDto refreshToken(RefreshTokenRequestDto refreshTokenRequestDto) {
    String refreshToken = refreshTokenRequestDto.getRefreshToken();
    if (!jwtUtil.isRefreshToken(refreshToken)) {
      throw new AuthenticationException(INVALID_REFRESH_TOKEN);
    }

    String username = jwtUtil.extractUsername(refreshToken);
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new AuthenticationException(INVALID_REFRESH_TOKEN));

    String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getEmail(), 
        user.isOperator());
    String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername());

    return new JwtResponseDto(newAccessToken, newRefreshToken, user.getUsername(), 
        user.getEmail(), user.isOperator());
  }

  private void validateRegistrationInput(RegisterDto registerDto) {
    if (!StringUtils.hasText(registerDto.getEmail()) 
        || !isValidEmail(registerDto.getEmail())) {
      throw new ValidationException(INVALID_EMAIL);
    }

    if (!StringUtils.hasText(registerDto.getUsername()) 
        || !isValidUsername(registerDto.getUsername())) {
      throw new ValidationException(INVALID_USERNAME);
    }

    if (!StringUtils.hasText(registerDto.getPassword()) 
        || !isValidPassword(registerDto.getPassword())) {
      throw new ValidationException(INVALID_PASSWORD);
    }
  }

  private void checkForExistingUser(RegisterDto registerDto) {
    if (userRepository.existsByEmail(registerDto.getEmail())) {
      throw new DuplicateEmailException(String.format(EMAIL_EXISTS, registerDto.getEmail()));
    }

    if (userRepository.existsByUsername(registerDto.getUsername())) {
      throw new DuplicateUsernameException(
          String.format(USERNAME_EXISTS, registerDto.getUsername()));
    }
  }

  private User createUserFromDto(RegisterDto registerDto) {
    User user = new User();
    user.setUsername(registerDto.getUsername());
    user.setEmail(registerDto.getEmail());
    user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
    user.setOperator(false);
    return user;
  }

  private boolean isValidEmail(String email) {
    return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
  }

  private boolean isValidUsername(String username) {
    return username.length() >= MIN_USERNAME_LENGTH && username.length() <= MAX_USERNAME_LENGTH;
  }

  private boolean isValidPassword(String password) {
    return password.length() >= MIN_PASSWORD_LENGTH;
  }
} 