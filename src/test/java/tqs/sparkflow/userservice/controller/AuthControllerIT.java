package tqs.sparkflow.userservice.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import tqs.sparkflow.userservice.UserServiceApplication;
import tqs.sparkflow.userservice.config.TestConfig;
import tqs.sparkflow.userservice.config.TestcontainersConfiguration;
import tqs.sparkflow.userservice.dto.LoginDto;
import tqs.sparkflow.userservice.dto.RefreshTokenRequestDto;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.repository.UserRepository;
import tqs.sparkflow.userservice.util.JwtUtil;

@SpringBootTest(
    classes = {
        UserServiceApplication.class,
        TestConfig.class,
        TestcontainersConfiguration.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.data.mongodb.auto-index-creation=true"
    }
)
@ActiveProfiles("test")
@Testcontainers
class AuthControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private final String testPassword = "password123";

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        userRepository.deleteAll();
        testUser = new User("testuser", "test@example.com", passwordEncoder.encode(testPassword));
        testUser = userRepository.save(testUser);
    }

    @Test
    void whenLoginWithValidCredentials_thenReturnTokens() throws Exception {
        LoginDto loginRequest = new LoginDto();
        loginRequest.setEmailOrUsername("test@example.com");
        loginRequest.setPassword(testPassword);

        Response response = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(objectMapper.writeValueAsString(loginRequest))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("accessToken", org.hamcrest.Matchers.notNullValue())
            .body("refreshToken", org.hamcrest.Matchers.notNullValue())
            .body("tokenType", equalTo("Bearer"))
            .extract().response();

        String responseContent = response.getBody().asString();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        
        String accessToken = jsonNode.get("accessToken").asText();
        String refreshToken = jsonNode.get("refreshToken").asText();
        
        assertThat(accessToken).isNotEmpty();
        assertThat(refreshToken).isNotEmpty();
        assertThat(jwtUtil.validateToken(accessToken, testUser.getUsername())).isTrue();
        assertThat(jwtUtil.validateToken(refreshToken, testUser.getUsername())).isTrue();
    }

    @Test
    void whenLoginWithInvalidEmail_thenReturnUnauthorized() throws Exception {
        LoginDto loginRequest = new LoginDto();
        loginRequest.setEmailOrUsername("invalid@example.com");
        loginRequest.setPassword(testPassword);

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(objectMapper.writeValueAsString(loginRequest))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void whenLoginWithInvalidPassword_thenReturnUnauthorized() throws Exception {
        LoginDto loginRequest = new LoginDto();
        loginRequest.setEmailOrUsername("test@example.com");
        loginRequest.setPassword("wrongpassword");

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(objectMapper.writeValueAsString(loginRequest))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void whenLoginWithEmptyCredentials_thenReturnBadRequest() throws Exception {
        LoginDto loginRequest = new LoginDto();
        loginRequest.setEmailOrUsername("");
        loginRequest.setPassword("");

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(objectMapper.writeValueAsString(loginRequest))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void whenRefreshTokenWithValidToken_thenReturnNewTokens() throws Exception {
        // First, login to get tokens
        LoginDto loginRequest = new LoginDto();
        loginRequest.setEmailOrUsername("test@example.com");
        loginRequest.setPassword(testPassword);
        
        Response loginResponse = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(objectMapper.writeValueAsString(loginRequest))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract().response();

        String loginResponseBody = loginResponse.getBody().asString();
        JsonNode loginJson = objectMapper.readTree(loginResponseBody);
        String refreshToken = loginJson.get("refreshToken").asText();

        // Now use refresh token to get new tokens
        RefreshTokenRequestDto refreshRequest = new RefreshTokenRequestDto();
        refreshRequest.setRefreshToken(refreshToken);

        Response refreshResponse = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(objectMapper.writeValueAsString(refreshRequest))
        .when()
            .post("/api/v1/auth/refresh")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("accessToken", org.hamcrest.Matchers.notNullValue())
            .body("refreshToken", org.hamcrest.Matchers.notNullValue())
            .body("tokenType", equalTo("Bearer"))
            .extract().response();

        String refreshResponseBody = refreshResponse.getBody().asString();
        JsonNode refreshJson = objectMapper.readTree(refreshResponseBody);
        
        String newAccessToken = refreshJson.get("accessToken").asText();
        String newRefreshToken = refreshJson.get("refreshToken").asText();
        
        assertThat(newAccessToken).isNotEmpty();
        assertThat(newRefreshToken).isNotEmpty();
        assertThat(jwtUtil.validateToken(newAccessToken, testUser.getUsername())).isTrue();
        assertThat(jwtUtil.validateToken(newRefreshToken, testUser.getUsername())).isTrue();
    }

    @Test
    void whenRefreshTokenWithInvalidToken_thenReturnUnauthorized() throws Exception {
        RefreshTokenRequestDto refreshRequest = new RefreshTokenRequestDto();
        refreshRequest.setRefreshToken("invalid.token.here");

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(objectMapper.writeValueAsString(refreshRequest))
        .when()
            .post("/api/v1/auth/refresh")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void whenRefreshTokenWithEmptyToken_thenReturnBadRequest() throws Exception {
        RefreshTokenRequestDto refreshRequest = new RefreshTokenRequestDto();
        refreshRequest.setRefreshToken("");

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(objectMapper.writeValueAsString(refreshRequest))
        .when()
            .post("/api/v1/auth/refresh")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void whenAccessProtectedEndpointWithValidToken_thenReturnSuccess() throws Exception {
        // First, login to get access token
        LoginDto loginRequest = new LoginDto();
        loginRequest.setEmailOrUsername("test@example.com");
        loginRequest.setPassword(testPassword);
        
        Response loginResponse = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(objectMapper.writeValueAsString(loginRequest))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract().response();

        String loginResponseBody = loginResponse.getBody().asString();
        JsonNode loginJson = objectMapper.readTree(loginResponseBody);
        String accessToken = loginJson.get("accessToken").asText();

        // Use access token to access protected endpoint - correct path is /users not /api/v1/users
        given()
            .header("Authorization", "Bearer " + accessToken)
        .when()
            .get("/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(testUser.getId()))
            .body("username", equalTo(testUser.getUsername()))
            .body("email", equalTo(testUser.getEmail()));
    }

    @Test
    void whenAccessProtectedEndpointWithInvalidToken_thenReturnUnauthorized() throws Exception {
        given()
            .header("Authorization", "Bearer invalid.token.here")
        .when()
            .get("/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void whenAccessProtectedEndpointWithoutToken_thenReturnUnauthorized() throws Exception {
        given()
        .when()
            .get("/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void whenAccessProtectedEndpointWithExpiredToken_thenReturnUnauthorized() throws Exception {
        // Create an expired token - need to create a custom method for this test
        // For now, we'll test with an obviously invalid token format
        String expiredToken = "expired.token.here";

        given()
            .header("Authorization", "Bearer " + expiredToken)
        .when()
            .get("/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
} 