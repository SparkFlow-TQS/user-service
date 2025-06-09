package tqs.sparkflow.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tqs.sparkflow.userservice.util.JwtUtil;

/**
 * JWT authentication filter for processing JWT tokens in HTTP requests.
 * This filter intercepts incoming requests and validates JWT tokens in the Authorization header.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";
  
  private final JwtUtil jwtUtil;
  private final UserDetailsService userDetailsService;
  
  /**
   * Constructor for JwtAuthenticationFilter.
   * 
   * @param jwtUtil the JWT utility service
   * @param userDetailsService the user details service
   */
  public JwtAuthenticationFilter(final JwtUtil jwtUtil, 
                                 final UserDetailsService userDetailsService) {
    this.jwtUtil = jwtUtil;
    this.userDetailsService = userDetailsService;
  }

  /**
   * Filters incoming HTTP requests to extract and validate JWT tokens.
   * 
   * @param request the HTTP servlet request
   * @param response the HTTP servlet response
   * @param chain the filter chain
   * @throws ServletException if a servlet-related error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                FilterChain chain) throws ServletException, IOException {
        
    final String requestTokenHeader = request.getHeader("Authorization");

    String username = null;
    String jwtToken = null;

    // JWT Token is in the form "Bearer token". Remove Bearer word and get only the Token
    if (requestTokenHeader != null && requestTokenHeader.startsWith(BEARER_PREFIX)) {
      jwtToken = requestTokenHeader.substring(BEARER_PREFIX.length()).trim();
      if (!jwtToken.isEmpty()) {
        try {
          username = jwtUtil.extractUsername(jwtToken);
        } catch (Exception e) {
          logger.warn("Unable to get JWT Token or JWT Token has expired");
        }
      }
    }

    // Once we get the token validate it.
    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

        // if token is valid configure Spring Security to manually set authentication
        if (jwtUtil.validateToken(jwtToken, userDetails.getUsername())) {
          UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = 
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());
          usernamePasswordAuthenticationToken
              .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
          // After setting the Authentication in the context, we specify
          // that the current user is authenticated. So it passes the Spring Security 
          // Configurations successfully.
          SecurityContextHolder.getContext()
              .setAuthentication(usernamePasswordAuthenticationToken);
        }
      } catch (Exception e) {
        logger.warn("Unable to load user details for username: " + username);
      }
    }
    chain.doFilter(request, response);
  }
} 