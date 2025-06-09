package tqs.sparkflow.userservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import jakarta.servlet.ServletException;
import tqs.sparkflow.userservice.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    private static Stream<Arguments> invalidAuthorizationHeaderProvider() {
        return Stream.of(
            Arguments.of(null, "no authorization header"),
            Arguments.of("Basic sometoken", "authorization header without Bearer"),
            Arguments.of("Bearer ", "empty Bearer token"),
            Arguments.of("Bearer    ", "Bearer token with only spaces")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAuthorizationHeaderProvider")
    void whenInvalidAuthorizationHeader_thenContinueFilterChain(String authHeader, String description) throws ServletException, IOException {
        // Given
        if (authHeader != null) {
            request.addHeader("Authorization", authHeader);
        }

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtUtil, never()).extractUsername(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void whenValidJwtToken_thenSetAuthentication() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String username = "testuser";
        
        request.addHeader("Authorization", "Bearer " + token);
        
        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, username)).thenReturn(true);
        when(userDetails.getUsername()).thenReturn(username);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil).extractUsername(token);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtUtil).validateToken(token, username);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(username);
    }

    private static Stream<Arguments> invalidTokenProvider() {
        return Stream.of(
            Arguments.of("invalid.jwt.token", false, "Token validation returns false"),
            Arguments.of("malformed.jwt.token", true, "Token extraction throws exception")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidTokenProvider")
    void whenInvalidJwtToken_thenDoNotSetAuthentication(String token, boolean throwsException, String description) throws ServletException, IOException {
        String username = "testuser";
        
        request.addHeader("Authorization", "Bearer " + token);
        
        if (throwsException) {
            when(jwtUtil.extractUsername(token)).thenThrow(new RuntimeException("Invalid token"));
        } else {
            when(jwtUtil.extractUsername(token)).thenReturn(username);
            when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
            when(jwtUtil.validateToken(token, username)).thenReturn(false);
            when(userDetails.getUsername()).thenReturn(username);
        }

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil).extractUsername(token);
        if (!throwsException) {
            verify(userDetailsService).loadUserByUsername(username);
            verify(jwtUtil).validateToken(token, username);
        } else {
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            verify(jwtUtil, never()).validateToken(anyString(), anyString());
        }
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void whenUserAlreadyAuthenticated_thenDoNotProcessToken() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String username = "testuser";
        
        request.addHeader("Authorization", "Bearer " + token);
        
        // Set existing authentication
        SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                username, null, null));
        
        when(jwtUtil.extractUsername(token)).thenReturn(username);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil).extractUsername(token);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtUtil, never()).validateToken(anyString(), anyString());
    }

    @Test
    void whenUserDetailsServiceThrowsException_thenDoNotSetAuthentication() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String username = "testuser";
        
        request.addHeader("Authorization", "Bearer " + token);
        
        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username))
            .thenThrow(new RuntimeException("User not found"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtil).extractUsername(token);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtUtil, never()).validateToken(anyString(), anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

} 