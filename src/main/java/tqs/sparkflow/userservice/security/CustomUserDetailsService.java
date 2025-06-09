package tqs.sparkflow.userservice.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.repository.UserRepository;

/**
 * Custom implementation of UserDetailsService for loading user-specific data.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;
  
  /**
   * Constructor for CustomUserDetailsService.
   * 
   * @param userRepository the user repository
   */
  public CustomUserDetailsService(final UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Loads user details by username for authentication.
   *
   * @param username the username identifying the user
   * @return UserDetails containing user information
   * @throws UsernameNotFoundException if the user is not found
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    if (user.isOperator()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_OPERATOR"));
    } else {
      authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    }

    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getUsername())
        .password(user.getPassword())
        .authorities(authorities)
        .build();
  }
} 