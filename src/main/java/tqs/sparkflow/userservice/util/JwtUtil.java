package tqs.sparkflow.userservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT utility class for token generation, validation, and extraction.
 */
@Component
public class JwtUtil {

  private static final String REFRESH_TYPE = "refresh";
  private static final String EMAIL_CLAIM = "email";
  private static final String OPERATOR_CLAIM = "isOperator";
  private static final String TYPE_CLAIM = "type";
  private static final int MIN_SECRET_LENGTH = 32;

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration}")
  private Long jwtExpiration;

  @Value("${jwt.refresh-expiration}")
  private Long refreshExpiration;

  /**
   * Extracts username from token.
   *
   * @param token the JWT token
   * @return the username
   */
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /**
   * Extracts expiration date from token.
   *
   * @param token the JWT token
   * @return the expiration date
   */
  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /**
   * Extracts a specific claim from token.
   *
   * @param token the JWT token
   * @param claimsResolver function to resolve the claim
   * @param <T> the type of the claim
   * @return the claim value
   */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  /**
   * Extracts all claims from token.
   *
   * @param token the JWT token
   * @return all claims
   */
  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  /**
   * Checks if token is expired.
   *
   * @param token the JWT token
   * @return true if token is expired
   */
  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  /**
   * Generates an access token.
   *
   * @param username the username
   * @param email the email
   * @param isOperator the operator status
   * @return the generated token
   */
  public String generateToken(String username, String email, Boolean isOperator) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(EMAIL_CLAIM, email);
    claims.put(OPERATOR_CLAIM, isOperator);
    return createToken(claims, username, jwtExpiration);
  }

  /**
   * Generates a refresh token.
   *
   * @param username the username
   * @return the generated refresh token
   */
  public String generateRefreshToken(String username) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(TYPE_CLAIM, REFRESH_TYPE);
    return createToken(claims, username, refreshExpiration);
  }

  /**
   * Creates a token with given claims and expiration.
   *
   * @param claims the claims to include
   * @param subject the subject (username)
   * @param expiration the expiration time in milliseconds
   * @return the created token
   */
  private String createToken(Map<String, Object> claims, String subject, Long expiration) {
    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * Validates a token against a username.
   *
   * @param token the JWT token
   * @param username the username to validate against
   * @return true if token is valid
   */
  public Boolean validateToken(String token, String username) {
    try {
      final String extractedUsername = extractUsername(token);
      return (extractedUsername.equals(username) && !isTokenExpired(token));
    } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
      // Token is invalid (expired, malformed, etc.)
      return false;
    }
  }

  /**
   * Validates a token against a username, allowing exceptions to propagate.
   * This method is primarily for testing purposes where specific exceptions are expected.
   *
   * @param token the JWT token
   * @param username the username to validate against
   * @return true if token is valid
   * @throws io.jsonwebtoken.ExpiredJwtException if token is expired
   * @throws io.jsonwebtoken.JwtException if token is malformed or invalid
   */
  public Boolean validateTokenWithExceptions(String token, String username) {
    final String extractedUsername = extractUsername(token);
    return (extractedUsername.equals(username) && !isTokenExpired(token));
  }

  /**
   * Checks if token is a refresh token.
   *
   * @param token the JWT token
   * @return true if token is a refresh token
   */
  public Boolean isRefreshToken(String token) {
    try {
      Claims claims = extractAllClaims(token);
      return REFRESH_TYPE.equals(claims.get(TYPE_CLAIM));
    } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Extracts email from token.
   *
   * @param token the JWT token
   * @return the email
   */
  public String extractEmail(String token) {
    return extractClaim(token, claims -> claims.get(EMAIL_CLAIM, String.class));
  }

  /**
   * Extracts operator status from token.
   *
   * @param token the JWT token
   * @return the operator status
   */
  public Boolean extractIsOperator(String token) {
    return extractClaim(token, claims -> claims.get(OPERATOR_CLAIM, Boolean.class));
  }

  /**
   * Gets the signing key for token validation.
   *
   * @return the signing key
   */
  private SecretKey getSigningKey() {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }
} 