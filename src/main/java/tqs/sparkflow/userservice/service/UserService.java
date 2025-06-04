package tqs.sparkflow.userservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.ResourceNotFoundException;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

  @Autowired
  private UserRepository userRepository;

  public User createUser(User user) {
    if (userRepository.existsByEmail(user.getEmail())) {
      throw new DuplicateEmailException("Email already exists: " + user.getEmail());
    }
    return userRepository.save(user);
  }

  public User getUserById(String id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
  }

  public User getUserByEmail(String email) {
    return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
  }

  public User updateUser(String id, User userDetails) {
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

  public void deleteUser(String id) {
    if (!userRepository.existsById(id)) {
      throw new ResourceNotFoundException("User not found with id: " + id);
    }
    userRepository.deleteById(id);
  }

  public List<User> getAllUsers() {
    return userRepository.findAll();
  }
} 