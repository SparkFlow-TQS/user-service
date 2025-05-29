package tqs.sparkflow.user_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tqs.sparkflow.user_service.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    
    // Find all operators
    List<User> findByIsOperatorTrue();
    
    // Find all non-operators
    List<User> findByIsOperatorFalse();
    
    // Find by email or username (for login)
    @Query("{ $or: [ { 'email': ?0 }, { 'username': ?0 } ] }")
    Optional<User> findByEmailOrUsername(String emailOrUsername);
    
    // Find by partial username (case-insensitive)
    @Query("{ 'username': { $regex: ?0, $options: 'i' } }")
    List<User> findByUsernameContainingIgnoreCase(String username);
    
    // Find by partial email (case-insensitive)
    @Query("{ 'email': { $regex: ?0, $options: 'i' } }")
    List<User> findByEmailContainingIgnoreCase(String email);
} 