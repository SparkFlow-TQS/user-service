package tqs.sparkflow.userservice.service;

import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tqs.sparkflow.userservice.dto.UserCreateDto;
import tqs.sparkflow.userservice.dto.UserUpdateDto;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.DuplicateUsernameException;
import tqs.sparkflow.userservice.exception.ResourceNotFoundException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.repository.UserRepository;

/**
 * Service layer for managing users.
 * Handles business logic for user operations including creation, retrieval, updates, and deletion.
 */
@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Creates a new user.
   *
   * @param userDto the user data to create
   * @return the created user
   * @throws DuplicateEmailException if the email already exists
   * @throws DuplicateUsernameException if the username already exists
   */
  public User createUser(UserCreateDto userDto) {
    if (userRepository.existsByEmail(userDto.getEmail())) {
      throw new DuplicateEmailException("Email already exists: " + userDto.getEmail());
    }
    
    if (userRepository.existsByUsername(userDto.getUsername())) {
      throw new DuplicateUsernameException("Username already exists: " + userDto.getUsername());
    }
    
    User user = new User();
    user.setUsername(userDto.getUsername());
    user.setEmail(userDto.getEmail());
    user.setPassword(passwordEncoder.encode(userDto.getPassword()));
    user.setOperator(userDto.isOperator());
    
    return userRepository.save(user);
  }

  /**
   * Retrieves a user by their ID.
   *
   * @param id the user ID
   * @return the user
   * @throws ResourceNotFoundException if the user is not found
   */
  public User getUserById(String id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
  }

  /**
   * Retrieves a user by their email.
   *
   * @param email the user's email
   * @return the user
   * @throws ResourceNotFoundException if the user is not found
   */
  public User getUserByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
  }

  /**
   * Updates an existing user.
   *
   * @param id the user ID
   * @param userDetails the updated user details
   * @return the updated user
   * @throws ResourceNotFoundException if the user is not found
   * @throws DuplicateEmailException if the new email already exists
   * @throws DuplicateUsernameException if the new username already exists
   */
  public User updateUser(String id, UserUpdateDto userDetails) {
    User existingUser = getUserById(id);

    // Check if the new email is already taken by another user
    if (userDetails.getEmail() != null) {
      Optional<User> userWithSameEmail = userRepository.findByEmail(userDetails.getEmail());
      if (userWithSameEmail.isPresent() && !userWithSameEmail.get().getId().equals(id)) {
        throw new DuplicateEmailException("Email already exists: " + userDetails.getEmail());
      }
      existingUser.setEmail(userDetails.getEmail());
    }

    // Check if the new username is already taken by another user
    if (userDetails.getUsername() != null) {
      Optional<User> userWithSameUsername = 
          userRepository.findByUsername(userDetails.getUsername());
      if (userWithSameUsername.isPresent() && !userWithSameUsername.get().getId().equals(id)) {
        throw new DuplicateUsernameException(
            "Username already exists: " + userDetails.getUsername());
      }
      existingUser.setUsername(userDetails.getUsername());
    }

    // Update password if provided
    if (userDetails.getPassword() != null) {
      existingUser.setPassword(passwordEncoder.encode(userDetails.getPassword()));
    }

    // Update operator status
    existingUser.setOperator(userDetails.isOperator());

    return userRepository.save(existingUser);
  }

  /**
   * Deletes a user.
   *
   * @param id the user ID
   * @throws ResourceNotFoundException if the user is not found
   */
  public void deleteUser(String id) {
    if (!userRepository.existsById(id)) {
      throw new ResourceNotFoundException("User not found with id: " + id);
    }
    userRepository.deleteById(id);
  }

  /**
   * Retrieves all users.
   *
   * @return list of all users
   */
  public List<User> getAllUsers() {
    return userRepository.findAll();
  }
} 