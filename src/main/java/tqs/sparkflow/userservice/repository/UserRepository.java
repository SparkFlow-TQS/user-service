package tqs.sparkflow.userservice.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tqs.sparkflow.userservice.model.User;

/**
 * Repository interface for User entity operations.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {
  /**
   * Finds a user by email.
   *
   * @param email the email to search for
   * @return Optional containing the user if found
   */
  Optional<User> findByEmail(String email);

  /**
   * Finds a user by username.
   *
   * @param username the username to search for
   * @return Optional containing the user if found
   */
  Optional<User> findByUsername(String username);

  /**
   * Checks if a user exists with the given email.
   *
   * @param email the email to check
   * @return true if a user exists with the email
   */
  boolean existsByEmail(String email);

  /**
   * Checks if a user exists with the given username.
   *
   * @param username the username to check
   * @return true if a user exists with the username
   */
  boolean existsByUsername(String username);

  /**
   * Finds all users who are operators.
   *
   * @return list of operator users
   */
  List<User> findByIsOperatorTrue();

  /**
   * Finds all users who are not operators.
   *
   * @return list of non-operator users
   */
  List<User> findByIsOperatorFalse();

  /**
   * Finds a user by email or username (for login).
   *
   * @param emailOrUsername the email or username to search for
   * @return Optional containing the user if found
   */
  @Query("{ $or: [ { 'email': ?0 }, { 'username': ?0 } ] }")
  Optional<User> findByEmailOrUsername(String emailOrUsername);

  /**
   * Finds users by partial username (case-insensitive).
   *
   * @param username the partial username to search for
   * @return list of users with matching usernames
   */
  @Query("{ 'username': { $regex: ?0, $options: 'i' } }")
  List<User> findByUsernameContainingIgnoreCase(String username);

  /**
   * Finds users by partial email (case-insensitive).
   *
   * @param email the partial email to search for
   * @return list of users with matching emails
   */
  @Query("{ 'email': { $regex: ?0, $options: 'i' } }")
  List<User> findByEmailContainingIgnoreCase(String email);
} 