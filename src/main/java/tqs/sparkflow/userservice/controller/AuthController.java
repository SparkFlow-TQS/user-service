package tqs.sparkflow.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tqs.sparkflow.userservice.dto.JwtResponseDTO;
import tqs.sparkflow.userservice.dto.LoginDTO;
import tqs.sparkflow.userservice.dto.RefreshTokenRequestDTO;
import tqs.sparkflow.userservice.dto.RegisterDTO;
import tqs.sparkflow.userservice.exception.AuthenticationException;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.DuplicateUsernameException;
import tqs.sparkflow.userservice.exception.ValidationException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.service.AuthService;

/**
 * REST controller for authentication operations.
 * Provides endpoints for user login, registration, and token refresh.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "Authentication", description = "APIs for user authentication")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Authenticates a user with email/username and password.
   *
   * @param loginDto the login credentials
   * @return JWT response with access and refresh tokens
   */
  @Operation(summary = "User login", 
             description = "Authenticates a user with email/username and password")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Login successful",
          content = @Content(schema = @Schema(implementation = JwtResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Invalid credentials"),
      @ApiResponse(responseCode = "400", description = "Invalid input data")
  })
  @PostMapping("/login")
  public ResponseEntity<JwtResponseDTO> login(@Valid @RequestBody LoginDTO loginDto) {
    try {
      JwtResponseDTO response = authService.login(loginDto);
      return ResponseEntity.ok(response);
    } catch (AuthenticationException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (ValidationException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Refreshes an access token using a refresh token.
   *
   * @param refreshTokenRequest the refresh token request
   * @return JWT response with new access and refresh tokens
   */
  @Operation(summary = "Refresh token", 
             description = "Refreshes an access token using a refresh token")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
          content = @Content(schema = @Schema(implementation = JwtResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Invalid refresh token"),
      @ApiResponse(responseCode = "400", description = "Invalid input data")
  })
  @PostMapping("/refresh")
  public ResponseEntity<JwtResponseDTO> refreshToken(
        @Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequest) {
    try {
      JwtResponseDTO response = authService.refreshToken(refreshTokenRequest);
      return ResponseEntity.ok(response);
    } catch (AuthenticationException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (ValidationException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Registers a new user.
   *
   * @param registerDto the registration data
   * @return the created user
   */
  @Operation(summary = "User registration", description = "Registers a new user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "User registered successfully",
          content = @Content(schema = @Schema(implementation = User.class))),
      @ApiResponse(responseCode = "409", description = "Email or username already exists"),
      @ApiResponse(responseCode = "400", description = "Invalid input data")
  })
  @PostMapping("/register")
  public ResponseEntity<User> register(@Valid @RequestBody RegisterDTO registerDto) {
    try {
      User user = authService.register(registerDto);
      return ResponseEntity.status(HttpStatus.CREATED).body(user);
    } catch (DuplicateEmailException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    } catch (DuplicateUsernameException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    } catch (ValidationException e) {
      return ResponseEntity.badRequest().build();
    }
  }
} 