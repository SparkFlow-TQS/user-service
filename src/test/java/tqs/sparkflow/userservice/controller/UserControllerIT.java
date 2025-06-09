package tqs.sparkflow.userservice.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

import tqs.sparkflow.userservice.UserServiceApplication;
import tqs.sparkflow.userservice.config.TestConfig;
import tqs.sparkflow.userservice.config.TestcontainersConfiguration;
import tqs.sparkflow.userservice.dto.LoginDto;
import tqs.sparkflow.userservice.dto.UserCreateDto;
import tqs.sparkflow.userservice.dto.UserUpdateDto;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.response.Response;
import org.springframework.security.crypto.password.PasswordEncoder;

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
class UserControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User operatorUser;
    private RequestSpecification operatorRequestSpec;
    private RequestSpecification userRequestSpec;
    private String operatorToken;
    private String userToken;
    private final String testPassword = "password123";

    @BeforeEach
    void setUp() throws Exception {
        RestAssured.port = port;
        userRepository.deleteAll();
        
        // Create test users with encoded passwords
        testUser = new User("testuser", "test@example.com", passwordEncoder.encode(testPassword));
        operatorUser = new User("operator", "operator@example.com", passwordEncoder.encode(testPassword), true);
        
        userRepository.saveAll(java.util.List.of(testUser, operatorUser));
        
        // Get JWT tokens for both users
        operatorToken = getJwtToken("operator@example.com", testPassword);
        userToken = getJwtToken("test@example.com", testPassword);
        
        // Set up request specifications with JWT auth
        operatorRequestSpec = given()
            .header("Authorization", "Bearer " + operatorToken)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
            
        userRequestSpec = given()
            .header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
    }
    
    private String getJwtToken(String email, String password) throws Exception {
        LoginDto loginDto = new LoginDto();
        loginDto.setEmailOrUsername(email);
        loginDto.setPassword(password);
        
        Response response = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(objectMapper.writeValueAsString(loginDto))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract().response();
            
        String responseBody = response.getBody().asString();
        JsonNode jsonResponse = objectMapper.readTree(responseBody);
        return jsonResponse.get("accessToken").asText();
    }

    @Test
    void whenCreateUserWithValidData_thenReturnCreated() throws Exception {
        UserCreateDto createDTO = new UserCreateDto();
        createDTO.setUsername("newuser");
        createDTO.setEmail("newuser@example.com");
        createDTO.setPassword("password123");
        createDTO.setOperator(false);

        operatorRequestSpec
            .body(objectMapper.writeValueAsString(createDTO))
        .when()
            .post("/api/v1/users")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("username", equalTo("newuser"))
            .body("email", equalTo("newuser@example.com"))
            .body("operator", equalTo(false));
    }

    @Test
    void whenCreateUserWithExistingEmail_thenReturnConflict() throws Exception {
        UserCreateDto createDTO = new UserCreateDto();
        createDTO.setUsername("newuser");
        createDTO.setEmail("test@example.com"); // Email already exists
        createDTO.setPassword("password123");
        createDTO.setOperator(false);

        operatorRequestSpec
            .body(objectMapper.writeValueAsString(createDTO))
        .when()
            .post("/api/v1/users")
        .then()
            .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void whenCreateUserWithInvalidData_thenReturnBadRequest() throws Exception {
        UserCreateDto createDTO = new UserCreateDto();
        createDTO.setUsername(""); // Invalid username
        createDTO.setEmail("invalid-email"); // Invalid email
        createDTO.setPassword("123"); // Password too short
        createDTO.setOperator(false);

        operatorRequestSpec
            .body(objectMapper.writeValueAsString(createDTO))
        .when()
            .post("/api/v1/users")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void whenCreateUserAsRegularUser_thenReturnForbidden() throws Exception {
        UserCreateDto createDTO = new UserCreateDto();
        createDTO.setUsername("newuser");
        createDTO.setEmail("newuser@example.com");
        createDTO.setPassword("password123");
        createDTO.setOperator(false);

        userRequestSpec
            .body(objectMapper.writeValueAsString(createDTO))
        .when()
            .post("/api/v1/users")
        .then()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void whenGetUserById_thenReturnUser() {
        operatorRequestSpec
        .when()
            .get("/api/v1/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("username", equalTo("testuser"))
            .body("email", equalTo("test@example.com"))
            .body("operator", equalTo(false));
    }

    @Test
    void whenGetUserByNonExistentId_thenReturnNotFound() {
        operatorRequestSpec
        .when()
            .get("/api/v1/users/{id}", "507f1f77bcf86cd799439011")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void whenGetUserByIdAsRegularUser_thenReturnForbidden() {
        userRequestSpec
        .when()
            .get("/api/v1/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void whenGetUserByEmail_thenReturnUser() {
        operatorRequestSpec
        .when()
            .get("/api/v1/users/email/{email}", "test@example.com")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("username", equalTo("testuser"))
            .body("email", equalTo("test@example.com"))
            .body("operator", equalTo(false));
    }

    @Test
    void whenGetUserByNonExistentEmail_thenReturnNotFound() {
        operatorRequestSpec
        .when()
            .get("/api/v1/users/email/{email}", "nonexistent@example.com")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void whenGetAllUsers_thenReturnUserList() {
        operatorRequestSpec
        .when()
            .get("/api/v1/users")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("$", hasSize(2))
            .body("[0].username", equalTo("testuser"))
            .body("[1].username", equalTo("operator"));
    }

    @Test
    void whenGetAllUsersAsRegularUser_thenReturnForbidden() {
        userRequestSpec
        .when()
            .get("/api/v1/users")
        .then()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void whenUpdateUserWithValidData_thenReturnUpdatedUser() throws Exception {
        UserUpdateDto updateDTO = new UserUpdateDto();
        updateDTO.setUsername("updateduser");
        updateDTO.setEmail("updated@example.com");
        updateDTO.setOperator(true);

        operatorRequestSpec
            .body(objectMapper.writeValueAsString(updateDTO))
        .when()
            .put("/api/v1/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("username", equalTo("updateduser"))
            .body("email", equalTo("updated@example.com"))
            .body("operator", equalTo(true));
    }

    @Test
    void whenUpdateUserWithExistingEmail_thenReturnConflict() throws Exception {
        UserUpdateDto updateDTO = new UserUpdateDto();
        updateDTO.setUsername("updateduser");
        updateDTO.setEmail("operator@example.com"); // Email already exists
        updateDTO.setOperator(false);

        operatorRequestSpec
            .body(objectMapper.writeValueAsString(updateDTO))
        .when()
            .put("/api/v1/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void whenUpdateNonExistentUser_thenReturnNotFound() throws Exception {
        UserUpdateDto updateDTO = new UserUpdateDto();
        updateDTO.setUsername("updateduser");
        updateDTO.setEmail("updated@example.com");
        updateDTO.setOperator(false);

        operatorRequestSpec
            .body(objectMapper.writeValueAsString(updateDTO))
        .when()
            .put("/api/v1/users/{id}", "507f1f77bcf86cd799439011")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void whenUpdateUserAsRegularUser_thenReturnForbidden() throws Exception {
        UserUpdateDto updateDTO = new UserUpdateDto();
        updateDTO.setUsername("updateduser");
        updateDTO.setEmail("updated@example.com");
        updateDTO.setOperator(false);

        userRequestSpec
            .body(objectMapper.writeValueAsString(updateDTO))
        .when()
            .put("/api/v1/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void whenDeleteUser_thenReturnNoContent() {
        operatorRequestSpec
        .when()
            .delete("/api/v1/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void whenDeleteNonExistentUser_thenReturnNotFound() {
        operatorRequestSpec
        .when()
            .delete("/api/v1/users/{id}", "507f1f77bcf86cd799439011")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void whenDeleteUserAsRegularUser_thenReturnForbidden() {
        userRequestSpec
        .when()
            .delete("/api/v1/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    // This endpoint doesn't exist in UserController - removing test

    // This endpoint doesn't exist in UserController - removing test

    @Test
    void whenAccessProtectedEndpointWithoutAuthentication_thenReturnUnauthorized() {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
        .when()
            .get("/api/v1/users")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void whenGetProfileAsOperator_thenReturnProfileInfo() {
        operatorRequestSpec
        .when()
            .get("/api/v1/users/profile")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("username", equalTo("operator"))
            .body("authenticated", equalTo(true));
    }

    @Test
    void whenGetProfileAsUser_thenReturnProfileInfo() {
        userRequestSpec
        .when()
            .get("/api/v1/users/profile")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("username", equalTo("testuser"))
            .body("authenticated", equalTo(true));
    }

    @Test
    void whenAccessTestEndpointAsOperator_thenReturnTestMessage() {
        operatorRequestSpec
        .when()
            .get("/api/v1/users/test")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("message", equalTo("Access granted to protected endpoint"))
            .body("user", equalTo("operator"));
    }

    @Test
    void whenAccessTestEndpointAsUser_thenReturnTestMessage() {
        userRequestSpec
        .when()
            .get("/api/v1/users/test")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("message", equalTo("Access granted to protected endpoint"))
            .body("user", equalTo("testuser"));
    }

    @Test
    void whenAccessProfileWithoutAuthentication_thenReturnUnauthorized() {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
        .when()
            .get("/api/v1/users/profile")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void whenAccessTestEndpointWithoutAuthentication_thenReturnUnauthorized() {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
        .when()
            .get("/api/v1/users/test")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
} 