package tqs.sparkflow.userservice.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import tqs.sparkflow.userservice.model.User;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        
        // Set test properties using reflection - HS512 requires 64+ bytes (512+ bits)
        ReflectionTestUtils.setField(jwtUtil, "secret", "testSecretKeyThatIsLongEnoughForHS512AlgorithmAndMeetsSecurityRequirementsWithAtLeast64Bytes");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 86400000L); // 24 hours
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604800000L); // 7 days
        
        testUser = new User("testuser", "test@example.com", "password");
        testUser.setId("1");
        testUser.setOperator(false);
    }

    @Test
    void whenGenerateToken_thenReturnValidToken() {
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts separated by dots
    }

    @Test
    void whenGenerateRefreshToken_thenReturnValidToken() {
        String refreshToken = jwtUtil.generateRefreshToken(testUser.getUsername());
        
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(refreshToken.split("\\.")).hasSize(3);
    }

    @Test
    void whenExtractUsernameFromValidToken_thenReturnUsername() {
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        
        String username = jwtUtil.extractUsername(token);
        
        assertThat(username).isEqualTo(testUser.getUsername());
    }

    @Test
    void whenExtractEmailFromValidToken_thenReturnEmail() {
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        
        String email = jwtUtil.extractEmail(token);
        
        assertThat(email).isEqualTo(testUser.getEmail());
    }

    @Test
    void whenExtractIsOperatorFromValidToken_thenReturnIsOperator() {
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        
        Boolean isOperator = jwtUtil.extractIsOperator(token);
        
        assertThat(isOperator).isEqualTo(testUser.isOperator());
    }

    @Test
    void whenExtractExpirationFromValidToken_thenReturnFutureDate() {
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        
        Date expiration = jwtUtil.extractExpiration(token);
        
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void whenValidateTokenWithValidToken_thenReturnTrue() {
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        
        Boolean isValid = jwtUtil.validateToken(token, testUser.getUsername());
        
        assertThat(isValid).isTrue();
    }

    @Test
    void whenValidateTokenWithWrongUsername_thenReturnFalse() {
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        
        Boolean isValid = jwtUtil.validateToken(token, "wrongusername");
        
        assertThat(isValid).isFalse();
    }

    @Test
    void whenValidateTokenWithExpiredToken_thenReturnFalse() {
        // Set very short expiration time
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 1L); // 1 millisecond
        
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // The validateToken method should handle expired tokens gracefully
        Boolean isValid = jwtUtil.validateToken(token, testUser.getUsername());
        
        assertThat(isValid).isFalse();
    }

    @Test
    void whenIsRefreshTokenWithRefreshToken_thenReturnTrue() {
        String refreshToken = jwtUtil.generateRefreshToken(testUser.getUsername());
        
        Boolean isRefresh = jwtUtil.isRefreshToken(refreshToken);
        
        assertThat(isRefresh).isTrue();
    }

    @Test
    void whenIsRefreshTokenWithAccessToken_thenReturnFalse() {
        String accessToken = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        
        Boolean isRefresh = jwtUtil.isRefreshToken(accessToken);
        
        assertThat(isRefresh).isFalse();
    }

    @Test
    void whenExtractUsernameFromMalformedToken_thenThrowException() {
        String malformedToken = "invalid.token.format";
        
        assertThatThrownBy(() -> jwtUtil.extractUsername(malformedToken))
            .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void whenExtractUsernameFromTokenWithInvalidSignature_thenThrowException() {
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        String tamperedToken = token.substring(0, token.length() - 5) + "tamper";
        
        assertThatThrownBy(() -> jwtUtil.extractUsername(tamperedToken))
            .isInstanceOf(SignatureException.class);
    }

    @Test
    void whenExtractUsernameFromExpiredToken_thenThrowException() {
        // Set very short expiration time
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 1L);
        
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertThatThrownBy(() -> jwtUtil.extractUsername(token))
            .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void whenGenerateTokenForOperator_thenTokenContainsOperatorFlag() {
        User operatorUser = new User("operator", "operator@example.com", "password");
        operatorUser.setOperator(true);
        
        String token = jwtUtil.generateToken(operatorUser.getUsername(), operatorUser.getEmail(), operatorUser.isOperator());
        
        Boolean isOperator = jwtUtil.extractIsOperator(token);
        assertThat(isOperator).isTrue();
    }

    @Test
    void whenGenerateTokenForRegularUser_thenTokenContainsRegularUserFlag() {
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        
        Boolean isOperator = jwtUtil.extractIsOperator(token);
        assertThat(isOperator).isFalse();
    }

    @Test
    void whenTokensGeneratedForSameUser_thenHaveSameUserInfo() {
        String token1 = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        String token2 = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        
        assertThat(jwtUtil.extractUsername(token1)).isEqualTo(jwtUtil.extractUsername(token2));
        assertThat(jwtUtil.extractEmail(token1)).isEqualTo(jwtUtil.extractEmail(token2));
        assertThat(jwtUtil.extractIsOperator(token1)).isEqualTo(jwtUtil.extractIsOperator(token2));
    }

    @Test
    void whenRefreshTokenHasLongerExpirationThanAccessToken_thenExpirationDatesAreCorrect() {
        String accessToken = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        String refreshToken = jwtUtil.generateRefreshToken(testUser.getUsername());
        
        Date accessTokenExpiration = jwtUtil.extractExpiration(accessToken);
        Date refreshTokenExpiration = jwtUtil.extractExpiration(refreshToken);
        
        assertThat(refreshTokenExpiration).isAfter(accessTokenExpiration);
    }

    @Test
    void whenExtractEmailFromRefreshToken_thenReturnNull() {
        String refreshToken = jwtUtil.generateRefreshToken(testUser.getUsername());
        
        String email = jwtUtil.extractEmail(refreshToken);
        
        assertThat(email).isNull();
    }

    @Test
    void whenExtractIsOperatorFromRefreshToken_thenReturnNull() {
        String refreshToken = jwtUtil.generateRefreshToken(testUser.getUsername());
        
        Boolean isOperator = jwtUtil.extractIsOperator(refreshToken);
        
        assertThat(isOperator).isNull();
    }

    @Test
    void whenIsRefreshTokenWithInvalidToken_thenReturnFalse() {
        String invalidToken = "invalid.token.format";
        
        Boolean isRefresh = jwtUtil.isRefreshToken(invalidToken);
        
        assertThat(isRefresh).isFalse();
    }

    @Test
    void whenValidateTokenWithExceptionsWithValidToken_thenReturnTrue() {
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        
        Boolean isValid = jwtUtil.validateTokenWithExceptions(token, testUser.getUsername());
        
        assertThat(isValid).isTrue();
    }

    @Test
    void whenValidateTokenWithExceptionsWithInvalidToken_thenThrowException() {
        String invalidToken = "invalid.token.format";
        
        assertThatThrownBy(() -> jwtUtil.validateTokenWithExceptions(invalidToken, testUser.getUsername()))
            .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void whenValidateTokenWithExceptionsWithExpiredToken_thenThrowException() {
        // Set very short expiration time
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 1L);
        
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getEmail(), testUser.isOperator());
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertThatThrownBy(() -> jwtUtil.validateTokenWithExceptions(token, testUser.getUsername()))
            .isInstanceOf(ExpiredJwtException.class);
    }
} 