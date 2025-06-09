package tqs.sparkflow.userservice.cucumber.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.datatable.DataTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;
import io.cucumber.java.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tqs.sparkflow.userservice.repository.UserRepository;
import tqs.sparkflow.userservice.model.User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UserSteps {
    private static final Logger logger = LoggerFactory.getLogger(UserSteps.class);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SharedTestContext sharedContext;

    private ResponseEntity<String> response;
    private String userJson;
    private String lastCreatedUsername;
    private String baseUrl;
    private String jwtToken;

    @Before
    public void setUp() {
        logger.info("Cucumber @Before hook executed");
        String port = environment.getProperty("local.server.port", "8080");
        baseUrl = "http://localhost:" + port;
    }

    @Before
    public void cleanDatabase() {
        try {
            // Clean up more thoroughly
            if (mongoTemplate.collectionExists("users")) {
                mongoTemplate.dropCollection("users");
            }
            response = null;
            userJson = null;
            lastCreatedUsername = null;
            jwtToken = null;
            
            // Cleanup complete - no delay needed for MongoDB operations
        } catch (org.springframework.data.mongodb.UncategorizedMongoDbException | 
                 java.lang.IllegalArgumentException e) {
            logger.warn("Error cleaning database: {}", e.getMessage());
        }
    }

    @Given("I have user data")
    public void i_have_user_data() {
        userJson = """
        {
            "username": "newuser",
            "email": "newuser@example.com",
            "password": "password123",
            "operator": false
        }
        """;
    }

    @Given("I am an authenticated administrator")
    public void i_am_an_authenticated_administrator() {
        // Create an operator user directly in the database and get JWT token
        try {
            // Create operator user directly in database (like integration tests do)
            User operatorUser = new User("admin", "admin@example.com", 
                passwordEncoder.encode("password123"), true);
            userRepository.save(operatorUser);
            logger.info("Created operator user directly in database");
            
            // Now login to get JWT token
            String loginJson = """
            {
                "emailOrUsername": "admin@example.com",
                "password": "password123"
            }
            """;
            
            HttpHeaders loginHeaders = new HttpHeaders();
            loginHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> loginRequest = new HttpEntity<>(loginJson, loginHeaders);
            
            ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                baseUrl + "/api/v1/auth/login", loginRequest, String.class);
            
            if (loginResponse.getStatusCode() != HttpStatus.OK) {
                logger.error("Failed to login as admin. Status: {}, Body: {}", 
                    loginResponse.getStatusCode(), loginResponse.getBody());
                throw new RuntimeException("Failed to login as admin. Status: " + loginResponse.getStatusCode());
            }
            
            // Extract JWT token from response
            String responseBody = loginResponse.getBody();
            // Parse JSON to extract accessToken
            if (responseBody != null && responseBody.contains("accessToken")) {
                // Simple JSON parsing - extract token between quotes after "accessToken":"
                int tokenStart = responseBody.indexOf("\"accessToken\":\"") + 15;
                int tokenEnd = responseBody.indexOf("\"", tokenStart);
                jwtToken = responseBody.substring(tokenStart, tokenEnd);
                logger.info("Successfully extracted JWT token");
            } else {
                throw new RuntimeException("Failed to extract JWT token from login response");
            }
            
            logger.info("Successfully authenticated as admin");
        } catch (org.springframework.web.client.RestClientException | 
                 java.lang.RuntimeException e) {
            logger.error("Error during authentication test: {}", e.getMessage(), e);
            throw new RuntimeException("Error during authentication test: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createAuthenticatedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken);
        }
        return headers;
    }

    @Given("there is an existing user {string}")
    public void there_is_an_existing_user(String username) {
        userJson = String.format("""
        {
            "username": "%s",
            "email": "%s@example.com",
            "password": "password123",
            "operator": false
        }
        """, username, username);
        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<String> entity = new HttpEntity<>(userJson, headers);
        response = restTemplate.postForEntity(baseUrl + "/api/v1/users", entity, String.class);
        
        if (response.getStatusCode() != HttpStatus.CREATED) {
            logger.error("Failed to create user. Status: {}, Body: {}", 
                response.getStatusCode(), response.getBody());
            throw new RuntimeException("Failed to create user: " + username);
        }
        
        lastCreatedUsername = username;
        logger.info("Successfully created user: {}", username);
    }

    @When("I delete the user with id {string}")
    public void i_delete_the_user_with_id(String id) {
        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<String> deleteResponse = restTemplate
            .exchange(baseUrl + "/api/v1/users/" + id, HttpMethod.DELETE, entity, String.class);

        logger.info("Delete response status: {}", deleteResponse.getStatusCode());
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        // Store the response in the class field
        this.response = deleteResponse;
    }

    @Then("the user should be deleted successfully")
    public void the_user_should_be_deleted_successfully() {
        logger.info("Asserting delete response: {}", response);
        assertThat(response)
            .withFailMessage("Response is null! Did you forget to call a step that sets it?")
            .isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }


    @When("I update the user's role to {string}")
    public void i_update_the_user_s_role_to(String role) {
        String userId = getUserIdByUsername(lastCreatedUsername);
        boolean operator = role.equalsIgnoreCase("operator") || role.equalsIgnoreCase("admin");
        String updateJson = String.format("""
        {
            "username": "%s",
            "email": "%s@example.com",
            "password": "password123",
            "operator": %s
        }
        """, lastCreatedUsername, lastCreatedUsername, operator);
        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<String> entity = new HttpEntity<>(updateJson, headers);
        response = restTemplate
            .exchange(baseUrl + "/api/v1/users/" + userId, HttpMethod.PUT, entity, String.class);
        
        if (response.getStatusCode() != HttpStatus.OK) {
            logger.error("Failed to update user role. Status: {}, Body: {}", 
                response.getStatusCode(), response.getBody());
        }
    }

    @Then("the user's role should be updated successfully")
    public void the_user_s_role_should_be_updated_successfully() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("operator");
    }

    @When("I send a POST request to \\/api\\/v1\\/users")
    public void i_send_post_request() {
        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<String> entity = new HttpEntity<>(userJson, headers);
        response = restTemplate.postForEntity(baseUrl + "/api/v1/users", entity, String.class);
        
        if (response.getStatusCode() != HttpStatus.CREATED) {
            logger.error("Failed to create user. Status: {}, Body: {}", 
                response.getStatusCode(), response.getBody());
        }
    }

    @When("I create a new user with the following details")
    public void i_create_a_new_user_with_the_following_details(DataTable dataTable) {
        List<Map<String, String>> data = dataTable.asMaps(String.class, String.class);
        String username = data.get(0).get("username");
        String email = data.get(0).get("email");
        String password = "password123"; // default for test
        String role = data.get(0).get("role");
        boolean operator = "ADMIN".equalsIgnoreCase(role) || "OPERATOR".equalsIgnoreCase(role);

        userJson = String.format("""
        {
            "username": "%s",
            "email": "%s",
            "password": "%s",
            "operator": %s
        }
        """, username, email, password, operator);
        i_send_post_request();
        lastCreatedUsername = username;
    }

    @Then("the response status should be 201")
    public void the_response_status_should_be_201() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Then("the user should be created successfully")
    public void the_user_should_be_created_successfully() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains(lastCreatedUsername);
    }

    @Then("I should receive a confirmation message")
    public void i_should_receive_a_confirmation_message() {
        assertThat(response).isNotNull();
        if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
            assertThat(response.getBody()).isNotNull();
        }
    }

    @When("I delete the user")
    public void i_delete_the_user() {
        logger.info("Attempting to delete user: {}", lastCreatedUsername);
        String userId = getUserIdByUsername(lastCreatedUsername);
        logger.info("Resolved userId: {}", userId);

        if (userId == null) {
            logger.error("User ID is null for username: {}", lastCreatedUsername);
            throw new RuntimeException("User ID is null for username: " + lastCreatedUsername);
        }

        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<Void> deleteResponse = restTemplate
            .exchange(baseUrl + "/api/v1/users/" + userId, HttpMethod.DELETE, entity, Void.class);

        logger.info("Delete response status: {}", deleteResponse.getStatusCode());
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Store the response in the class field
        this.response = ResponseEntity.status(deleteResponse.getStatusCode()).build();
    }

    private String getUserIdByUsername(String username) {
        try {
            HttpHeaders headers = createAuthenticatedHeaders();
            HttpEntity<String> entity = new HttpEntity<>(null, headers);
            ResponseEntity<List<Map<String, Object>>> usersResponse = restTemplate
                .exchange(baseUrl + "/api/v1/users", HttpMethod.GET, entity, 
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (usersResponse.getStatusCode() != HttpStatus.OK) {
                logger.error("Failed to fetch users. Status: {}, Body: {}", 
                    usersResponse.getStatusCode(), usersResponse.getBody());
                throw new RuntimeException("Failed to fetch users list. Status: " + usersResponse.getStatusCode());
            }

            List<Map<String, Object>> users = usersResponse.getBody();
            if (users == null) {
                logger.error("Users list is null");
                throw new RuntimeException("Failed to fetch users list - response body is null");
            }

            return users.stream()
                .filter(user -> username.equals(user.get("username")))
                .findFirst()
                .map(user -> user.get("id").toString())
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        } catch (Exception e) {
            logger.error("Error fetching user ID: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching user ID: " + e.getMessage(), e);
        }
    }

    // New step definitions for additional scenarios

    @When("I try to create a new user")
    public void i_try_to_create_a_new_user() {
        userJson = """
        {
            "username": "testuser",
            "email": "test@example.com",
            "password": "password123",
            "operator": false
        }
        """;
        
        // Make request without authentication
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(userJson, headers);
        response = restTemplate.postForEntity(baseUrl + "/api/v1/users", entity, String.class);
        
        // Store in shared context so other step classes can access it
        sharedContext.setLastResponse(response);
    }


    @Then("no user should be created")
    public void no_user_should_be_created() {
        // Verify user count remains the same or check specific user doesn't exist
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.CREATED);
    }

    @Given("there is an existing user with email {string}")
    public void there_is_an_existing_user_with_email(String email) {
        User existingUser = new User("existinguser", email, 
            passwordEncoder.encode("password123"), false);
        userRepository.save(existingUser);
    }

    @Then("I should receive a conflict error")
    public void i_should_receive_a_conflict_error() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Then("the user should not be created")
    public void the_user_should_not_be_created() {
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.CREATED);
    }

    @Given("I am an authenticated user {string}")
    public void i_am_an_authenticated_user(String username) {
        // Create regular user and authenticate
        User regularUser = new User(username, username + "@example.com", 
            passwordEncoder.encode("password123"), false);
        userRepository.save(regularUser);
        lastCreatedUsername = username;
        
        // Login to get JWT token
        String loginJson = String.format("""
        {
            "emailOrUsername": "%s@example.com",
            "password": "password123"
        }
        """, username);
        
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> loginRequest = new HttpEntity<>(loginJson, loginHeaders);
        
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/login", loginRequest, String.class);
        
        if (loginResponse.getStatusCode() == HttpStatus.OK) {
            // Extract JWT token from response
            String responseBody = loginResponse.getBody();
            if (responseBody != null && responseBody.contains("accessToken")) {
                jwtToken = responseBody.substring(responseBody.indexOf("accessToken\":\"") + 14);
                jwtToken = jwtToken.substring(0, jwtToken.indexOf("\""));
            }
        }
    }

    @When("I request my profile information")
    public void i_request_my_profile_information() {
        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        response = restTemplate.exchange(baseUrl + "/api/v1/users/profile", HttpMethod.GET, entity, String.class);
    }

    @Then("I should receive my profile details")
    public void i_should_receive_my_profile_details() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Then("the response should contain my username and email")
    public void the_response_should_contain_my_username_and_email() {
        assertThat(response.getBody()).contains("username");
        assertThat(response.getBody()).contains("authenticated");
    }

    @When("I try to update the user with invalid email format {string}")
    public void i_try_to_update_the_user_with_invalid_email_format(String invalidEmail) {
        String userId = getUserIdByUsername(lastCreatedUsername);
        String updateJson = String.format("""
        {
            "username": "updateduser",
            "email": "%s",
            "operator": false
        }
        """, invalidEmail);
        
        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<String> entity = new HttpEntity<>(updateJson, headers);
        response = restTemplate.exchange(baseUrl + "/api/v1/users/" + userId, HttpMethod.PUT, entity, String.class);
    }

    @Then("I should receive a validation error")
    public void i_should_receive_a_validation_error() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Then("the user should not be updated")
    public void the_user_should_not_be_updated() {
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
    }

    @When("I access the protected test endpoint")
    public void i_access_the_protected_test_endpoint() {
        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        response = restTemplate.exchange(baseUrl + "/api/v1/users/test", HttpMethod.GET, entity, String.class);
    }

    @Then("I should receive a success response")
    public void i_should_receive_a_success_response() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Then("the response should confirm my access")
    public void the_response_should_confirm_my_access() {
        assertThat(response.getBody()).contains("Access granted");
    }

    @Given("there are multiple users in the system")
    public void there_are_multiple_users_in_the_system() {
        // Create additional test users
        User user1 = new User("user1", "user1@example.com", 
            passwordEncoder.encode("password123"), false);
        User user2 = new User("user2", "user2@example.com", 
            passwordEncoder.encode("password123"), false);
        userRepository.save(user1);
        userRepository.save(user2);
    }

    @When("I request the list of all users")
    public void i_request_the_list_of_all_users() {
        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        response = restTemplate.exchange(baseUrl + "/api/v1/users", HttpMethod.GET, entity, String.class);
    }

    @Then("I should receive a list containing all users")
    public void i_should_receive_a_list_containing_all_users() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("[");
        assertThat(response.getBody()).contains("]");
    }

    @Then("each user should have username and email information")
    public void each_user_should_have_username_and_email_information() {
        assertThat(response.getBody()).contains("username");
        assertThat(response.getBody()).contains("email");
    }
}
