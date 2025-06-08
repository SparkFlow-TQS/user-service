package tqs.sparkflow.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tqs.sparkflow.userservice.dto.UserCreateDTO;
import tqs.sparkflow.userservice.dto.UserUpdateDTO;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.ResourceNotFoundException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.service.UserService;

/**
 * REST controller for managing users.
 * Provides endpoints for creating, reading, updating, and deleting users.
 */
@RestController
@RequestMapping("/users")
@Validated
@Tag(name = "User Management", description = "APIs for managing users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * Creates a new user.
   *
   * @param userDto the user data to create
   * @return the created user with HTTP 201 status
   * @throws DuplicateEmailException if the email already exists
   */
  @Operation(summary = "Create a new user", 
             description = "Creates a new user with the provided details")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "User created successfully",
        content = @Content(schema = @Schema(implementation = User.class))),
      @ApiResponse(responseCode = "409", description = "Email already exists"),
      @ApiResponse(responseCode = "400", description = "Invalid input data")
  })
  @PostMapping
  public ResponseEntity<User> createUser(@Valid @RequestBody UserCreateDTO userDto) {
    try {
      User savedUser = userService.createUser(userDto);
      return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    } catch (DuplicateEmailException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  /**
   * Retrieves a user by their ID.
   *
   * @param id the user ID
   * @return the user with HTTP 200 status
   * @throws ResourceNotFoundException if the user is not found
   */
  @Operation(summary = "Get user by ID", 
             description = "Retrieves a user by their unique identifier")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User found",
        content = @Content(schema = @Schema(implementation = User.class))),
      @ApiResponse(responseCode = "404", description = "User not found")
  })
  @GetMapping("/{id}")
  public ResponseEntity<User> getUserById(
        @Parameter(description = "ID of the user to retrieve") @PathVariable String id) {
    try {
      User user = userService.getUserById(id);
      return ResponseEntity.ok(user);
    } catch (ResourceNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Retrieves a user by their email.
   *
   * @param email the user's email
   * @return the user with HTTP 200 status
   * @throws ResourceNotFoundException if the user is not found
   */
  @Operation(summary = "Get user by email", 
             description = "Retrieves a user by their email address")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User found",
        content = @Content(schema = @Schema(implementation = User.class))),
      @ApiResponse(responseCode = "404", description = "User not found")
  })
  @GetMapping("/email/{email}")
  public ResponseEntity<User> getUserByEmail(
        @Parameter(description = "Email of the user to retrieve") @PathVariable String email) {
    try {
      User user = userService.getUserByEmail(email);
      return ResponseEntity.ok(user);
    } catch (ResourceNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Updates an existing user.
   *
   * @param id the user ID
   * @param userDetails the updated user details
   * @return the updated user with HTTP 200 status
   * @throws ResourceNotFoundException if the user is not found
   * @throws DuplicateEmailException if the new email already exists
   */
  @Operation(summary = "Update user", description = "Updates an existing user's details")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User updated successfully",
        content = @Content(schema = @Schema(implementation = User.class))),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "409", description = "Email already exists"),
      @ApiResponse(responseCode = "400", description = "Invalid input data")
  })
  @PutMapping("/{id}")
  public ResponseEntity<User> updateUser(
        @Parameter(description = "ID of the user to update") @PathVariable String id,
        @Valid @RequestBody UserUpdateDTO userDetails) {
    try {
      User updatedUser = userService.updateUser(id, userDetails);
      return ResponseEntity.ok(updatedUser);
    } catch (ResourceNotFoundException e) {
      return ResponseEntity.notFound().build();
    } catch (DuplicateEmailException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  /**
   * Deletes a user.
   *
   * @param id the user ID
   * @return HTTP 204 status if successful
   * @throws ResourceNotFoundException if the user is not found
   */
  @Operation(summary = "Delete user", description = "Deletes a user by their ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "User deleted successfully"),
      @ApiResponse(responseCode = "404", description = "User not found")
  })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(
        @Parameter(description = "ID of the user to delete") @PathVariable String id) {
    try {
      userService.deleteUser(id);
      return ResponseEntity.noContent().build();
    } catch (ResourceNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Retrieves all users.
   *
   * @return list of all users with HTTP 200 status
   */
  @Operation(summary = "Get all users", description = "Retrieves a list of all users")
  @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of users retrieved successfully",
            content = @Content(schema = @Schema(implementation = User.class)))
  })
  @GetMapping
  public ResponseEntity<List<User>> getAllUsers() {
    List<User> users = userService.getAllUsers();
    return ResponseEntity.ok(users);
  }

  /**
   * Gets the current user's profile information.
   *
   * @return the user's profile information with HTTP 200 status
   */
  @Operation(summary = "Get user profile", 
             description = "Gets the current user's profile information")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping("/profile")
  public ResponseEntity<Map<String, Object>> getProfile() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    Map<String, Object> profile = new HashMap<>();
    profile.put("username", authentication.getName());
    profile.put("authorities", authentication.getAuthorities());
    profile.put("authenticated", authentication.isAuthenticated());
    
    return ResponseEntity.ok(profile);
  }

  /**
   * Tests JWT authentication on a protected endpoint.
   *
   * @return a test response with HTTP 200 status if authenticated
   */
  @Operation(summary = "Test protected endpoint", description = "Tests JWT authentication")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Access granted"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping("/test")
  public ResponseEntity<Map<String, String>> test() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    Map<String, String> response = new HashMap<>();
    response.put("message", "Access granted to protected endpoint");
    response.put("user", authentication.getName());
    
    return ResponseEntity.ok(response);
  }
} 