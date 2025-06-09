package tqs.sparkflow.userservice.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import tqs.sparkflow.userservice.config.TestcontainersConfiguration;

@SpringBootTest(classes = {
    tqs.sparkflow.userservice.UserServiceApplication.class,
    TestcontainersConfiguration.class
})
@ActiveProfiles("test")
@Testcontainers
class JwtUtilIT {

    @Autowired
    private JwtUtil jwtUtil;

    private String testUsername;
    private String testEmail;
    private Boolean isOperator;
    private String validToken;
    private String refreshToken;
    private String expiredToken;

    @BeforeEach
    void setUp() {
        testUsername = "testuser";
        testEmail = "test@example.com";
        isOperator = false;
        validToken = jwtUtil.generateToken(testUsername, testEmail, isOperator);
        refreshToken = jwtUtil.generateRefreshToken(testUsername);
        
        // Create an expired token for testing
        String secret = "testSecretKeyThatIsLongEnoughForHS512AlgorithmAndMeetsSecurityRequirementsWithAtLeast64Bytes";
        expiredToken = Jwts.builder()
                .subject(testUsername)
                .issuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24)) // 1 day ago
                .expiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // 1 hour ago
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test
    void whenGenerateToken_thenReturnValidToken() {
        String token = jwtUtil.generateToken(testUsername, testEmail, isOperator);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo(testUsername);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo(testEmail);
        assertThat(jwtUtil.extractIsOperator(token)).isEqualTo(isOperator);
    }

    @Test
    void whenGenerateRefreshToken_thenReturnValidRefreshToken() {
        String token = jwtUtil.generateRefreshToken(testUsername);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo(testUsername);
        assertThat(jwtUtil.isRefreshToken(token)).isTrue();
    }

    @Test
    void whenExtractUsername_thenReturnCorrectUsername() {
        String extractedUsername = jwtUtil.extractUsername(validToken);

        assertThat(extractedUsername).isEqualTo(testUsername);
    }

    @Test
    void whenExtractEmail_thenReturnCorrectEmail() {
        String extractedEmail = jwtUtil.extractEmail(validToken);

        assertThat(extractedEmail).isEqualTo(testEmail);
    }

    @Test
    void whenExtractIsOperator_thenReturnCorrectValue() {
        Boolean extractedIsOperator = jwtUtil.extractIsOperator(validToken);

        assertThat(extractedIsOperator).isEqualTo(isOperator);
    }

    @Test
    void whenExtractExpiration_thenReturnFutureDate() {
        Date expiration = jwtUtil.extractExpiration(validToken);

        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void whenValidateTokenWithValidToken_thenReturnTrue() {
        boolean isValid = jwtUtil.validateToken(validToken, testUsername);

        assertThat(isValid).isTrue();
    }

    @Test
    void whenValidateTokenWithWrongUsername_thenReturnFalse() {
        boolean isValid = jwtUtil.validateToken(validToken, "wronguser");

        assertThat(isValid).isFalse();
    }

    @Test
    void whenValidateTokenWithExpiredToken_thenThrowException() {
        assertThrows(ExpiredJwtException.class, () -> {
            jwtUtil.validateTokenWithExceptions(expiredToken, testUsername);
        });
    }

    @Test
    void whenIsRefreshTokenWithAccessToken_thenReturnFalse() {
        boolean isRefresh = jwtUtil.isRefreshToken(validToken);

        assertThat(isRefresh).isFalse();
    }

    @Test
    void whenIsRefreshTokenWithRefreshToken_thenReturnTrue() {
        boolean isRefresh = jwtUtil.isRefreshToken(refreshToken);

        assertThat(isRefresh).isTrue();
    }

    @Test
    void whenExtractClaimFromToken_thenReturnCorrectClaim() {
        String subject = jwtUtil.extractClaim(validToken, claims -> claims.getSubject());

        assertThat(subject).isEqualTo(testUsername);
    }

    @Test
    void whenGenerateTokenWithDifferentUsers_thenReturnDifferentTokens() {
        String token1 = jwtUtil.generateToken("user1", "user1@example.com", false);
        String token2 = jwtUtil.generateToken("user2", "user2@example.com", true);

        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtUtil.extractUsername(token1)).isEqualTo("user1");
        assertThat(jwtUtil.extractUsername(token2)).isEqualTo("user2");
        assertThat(jwtUtil.extractEmail(token1)).isEqualTo("user1@example.com");
        assertThat(jwtUtil.extractEmail(token2)).isEqualTo("user2@example.com");
        assertThat(jwtUtil.extractIsOperator(token1)).isFalse();
        assertThat(jwtUtil.extractIsOperator(token2)).isTrue();
    }

    @Test
    void whenTokenIsValid_thenExtractionMethodsWork() {
        String token = jwtUtil.generateToken(testUsername, testEmail, isOperator);

        // All these should work without throwing exceptions
        assertThat(jwtUtil.extractUsername(token)).isEqualTo(testUsername);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo(testEmail);
        assertThat(jwtUtil.extractIsOperator(token)).isEqualTo(isOperator);
        assertThat(jwtUtil.extractExpiration(token)).isAfter(new Date());
        assertThat(jwtUtil.validateToken(token, testUsername)).isTrue();
    }

    @Test
    void whenRefreshTokenIsValid_thenExtractionMethodsWork() {
        String token = jwtUtil.generateRefreshToken(testUsername);

        // All these should work without throwing exceptions
        assertThat(jwtUtil.extractUsername(token)).isEqualTo(testUsername);
        assertThat(jwtUtil.extractExpiration(token)).isAfter(new Date());
        assertThat(jwtUtil.validateToken(token, testUsername)).isTrue();
        assertThat(jwtUtil.isRefreshToken(token)).isTrue();
    }

    @Test
    void whenGenerateTokenWithOperatorTrue_thenExtractCorrectValue() {
        String token = jwtUtil.generateToken(testUsername, testEmail, true);

        assertThat(jwtUtil.extractIsOperator(token)).isTrue();
    }

    @Test
    void whenGenerateTokenWithOperatorFalse_thenExtractCorrectValue() {
        String token = jwtUtil.generateToken(testUsername, testEmail, false);

        assertThat(jwtUtil.extractIsOperator(token)).isFalse();
    }
} 