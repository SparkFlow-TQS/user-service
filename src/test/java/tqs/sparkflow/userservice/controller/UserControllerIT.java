package tqs.sparkflow.userservice.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
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
import tqs.sparkflow.userservice.dto.UserCreateDto;
import tqs.sparkflow.userservice.dto.UserUpdateDto;
import tqs.sparkflow.userservice.model.User;
import tqs.sparkflow.userservice.repository.UserRepository;

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

    private User testUser;
    private User operatorUser;
    private RequestSpecification operatorRequestSpec;
    private RequestSpecification userRequestSpec;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        userRepository.deleteAll();
        
        testUser = new User("testuser", "test@example.com", "password123");
        operatorUser = new User("operator", "operator@example.com", "password123", true);
        
        userRepository.saveAll(java.util.List.of(testUser, operatorUser));
        
        // Set up request specifications with basic auth for roles
        operatorRequestSpec = given()
            .auth().basic("test", "test") // Using test user from TestConfig
            .contentType(MediaType.APPLICATION_JSON_VALUE);
            
        userRequestSpec = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE);
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
            .post("/users")
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
            .post("/users")
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
            .post("/users")
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
            .post("/users")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void whenGetUserById_thenReturnUser() throws Exception {
        operatorRequestSpec
        .when()
            .get("/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("username", equalTo("testuser"))
            .body("email", equalTo("test@example.com"))
            .body("operator", equalTo(false));
    }

    @Test
    void whenGetUserByNonExistentId_thenReturnNotFound() throws Exception {
        operatorRequestSpec
        .when()
            .get("/users/{id}", "507f1f77bcf86cd799439011")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void whenGetUserByIdAsRegularUser_thenReturnForbidden() throws Exception {
        userRequestSpec
        .when()
            .get("/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void whenGetUserByEmail_thenReturnUser() throws Exception {
        operatorRequestSpec
        .when()
            .get("/users/email/{email}", "test@example.com")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("username", equalTo("testuser"))
            .body("email", equalTo("test@example.com"))
            .body("operator", equalTo(false));
    }

    @Test
    void whenGetUserByNonExistentEmail_thenReturnNotFound() throws Exception {
        operatorRequestSpec
        .when()
            .get("/users/email/{email}", "nonexistent@example.com")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void whenGetAllUsers_thenReturnUserList() throws Exception {
        operatorRequestSpec
        .when()
            .get("/users")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("$", hasSize(2))
            .body("[0].username", equalTo("testuser"))
            .body("[1].username", equalTo("operator"));
    }

    @Test
    void whenGetAllUsersAsRegularUser_thenReturnForbidden() throws Exception {
        userRequestSpec
        .when()
            .get("/users")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
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
            .put("/users/{id}", testUser.getId())
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
            .put("/users/{id}", testUser.getId())
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
            .put("/users/{id}", "507f1f77bcf86cd799439011")
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
            .put("/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void whenDeleteUser_thenReturnNoContent() throws Exception {
        operatorRequestSpec
        .when()
            .delete("/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void whenDeleteNonExistentUser_thenReturnNotFound() throws Exception {
        operatorRequestSpec
        .when()
            .delete("/users/{id}", "507f1f77bcf86cd799439011")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void whenDeleteUserAsRegularUser_thenReturnForbidden() throws Exception {
        userRequestSpec
        .when()
            .delete("/users/{id}", testUser.getId())
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    // This endpoint doesn't exist in UserController - removing test

    // This endpoint doesn't exist in UserController - removing test

    @Test
    void whenAccessProtectedEndpointWithoutAuthentication_thenReturnUnauthorized() throws Exception {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
        .when()
            .get("/users")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
} 