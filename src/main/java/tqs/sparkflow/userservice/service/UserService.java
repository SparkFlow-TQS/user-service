package tqs.sparkflow.userservice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import tqs.sparkflow.userservice.dto.UserCreateDTO;
import tqs.sparkflow.userservice.dto.UserUpdateDTO;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
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

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Creates a new user.
   *
   * @param userDTO the user data to create
   * @return the created user
   * @throws DuplicateEmailException if the email already exists
   */
  public User createUser(UserCreateDTO userDTO) {
    if (userRepository.existsByEmail(userDTO.getEmail())) {
      throw new DuplicateEmailException("Email already exists: " + userDTO.getEmail());
    }
    
    User user = new User();
    user.setUsername(userDTO.getUsername());
    user.setEmail(userDTO.getEmail());
    user.setPassword(userDTO.getPassword());
    user.setOperator(userDTO.isOperator());
    
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
   */
  public User updateUser(String id, UserUpdateDTO userDetails) {
    User existingUser = getUserById(id);

    // Check if the new email is already taken by another user
    Optional<User> userWithSameEmail = userRepository.findByEmail(userDetails.getEmail());
    if (userWithSameEmail.isPresent() && !userWithSameEmail.get().getId().equals(id)) {
      throw new DuplicateEmailException("Email already exists: " + userDetails.getEmail());
    }

    existingUser.setUsername(userDetails.getUsername());
    existingUser.setEmail(userDetails.getEmail());
    existingUser.setPassword(userDetails.getPassword());
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