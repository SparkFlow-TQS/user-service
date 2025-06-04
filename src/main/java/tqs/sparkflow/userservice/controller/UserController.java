package tqs.sparkflow.userservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.exception.DuplicateEmailException;
import tqs.sparkflow.userservice.exception.ResourceNotFoundException;
import tqs.sparkflow.userservice.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

  @Autowired
  private UserService userService;

  @PostMapping
  public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
    try {
      User savedUser = userService.createUser(user);
      return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    } catch (DuplicateEmailException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<User> getUserById(@PathVariable String id) {
    try {
      User user = userService.getUserById(id);
      return ResponseEntity.ok(user);
    } catch (ResourceNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/email/{email}")
  public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
    try {
      User user = userService.getUserByEmail(email);
      return ResponseEntity.ok(user);
    } catch (ResourceNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<User> updateUser(@PathVariable String id, @Valid @RequestBody User userDetails) {
    try {
      User updatedUser = userService.updateUser(id, userDetails);
      return ResponseEntity.ok(updatedUser);
    } catch (ResourceNotFoundException e) {
      return ResponseEntity.notFound().build();
    } catch (DuplicateEmailException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable String id) {
    try {
      userService.deleteUser(id);
      return ResponseEntity.noContent().build();
    } catch (ResourceNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping
  public ResponseEntity<List<User>> getAllUsers() {
    List<User> users = userService.getAllUsers();
    return ResponseEntity.ok(users);
  }
} 