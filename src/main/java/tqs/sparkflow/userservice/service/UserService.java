package tqs.sparkflow.userservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tqs.sparkflow.userservice.dto.CreateUserDto;
import tqs.sparkflow.userservice.dto.UserDto;
import tqs.sparkflow.userservice.entity.User;
import tqs.sparkflow.userservice.exception.UserAlreadyExistsException;
import tqs.sparkflow.userservice.exception.UserNotFoundException;
import tqs.sparkflow.userservice.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return UserDto.fromUser(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
        return UserDto.fromUser(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return UserDto.fromUser(user);
    }

    public UserDto createUser(CreateUserDto createUserDto) {
        // Check if username already exists
        if (userRepository.existsByUsername(createUserDto.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + createUserDto.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(createUserDto.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + createUserDto.getEmail());
        }

        // Create new user
        User user = new User();
        user.setUsername(createUserDto.getUsername());
        user.setEmail(createUserDto.getEmail());
        user.setPassword(passwordEncoder.encode(createUserDto.getPassword()));
        user.setFirstName(createUserDto.getFirstName());
        user.setLastName(createUserDto.getLastName());
        user.setRole(createUserDto.getRole());

        User savedUser = userRepository.save(user);
        return UserDto.fromUser(savedUser);
    }

    public UserDto updateUser(Long id, CreateUserDto updateUserDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        // Check if username is being changed and if it already exists
        if (!user.getUsername().equals(updateUserDto.getUsername()) && 
            userRepository.existsByUsername(updateUserDto.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + updateUserDto.getUsername());
        }

        // Check if email is being changed and if it already exists
        if (!user.getEmail().equals(updateUserDto.getEmail()) && 
            userRepository.existsByEmail(updateUserDto.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + updateUserDto.getEmail());
        }

        // Update user fields
        user.setUsername(updateUserDto.getUsername());
        user.setEmail(updateUserDto.getEmail());
        user.setFirstName(updateUserDto.getFirstName());
        user.setLastName(updateUserDto.getLastName());
        user.setRole(updateUserDto.getRole());

        // Only update password if provided
        if (updateUserDto.getPassword() != null && !updateUserDto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updateUserDto.getPassword()));
        }

        User savedUser = userRepository.save(user);
        return UserDto.fromUser(savedUser);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public UserDto enableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        
        user.setEnabled(true);
        User savedUser = userRepository.save(user);
        return UserDto.fromUser(savedUser);
    }

    public UserDto disableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        
        user.setEnabled(false);
        User savedUser = userRepository.save(user);
        return UserDto.fromUser(savedUser);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}