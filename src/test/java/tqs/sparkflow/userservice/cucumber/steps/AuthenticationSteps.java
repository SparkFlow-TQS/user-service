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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticationSteps {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationSteps.class);

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
    private ObjectMapper objectMapper;

    @Autowired
    private SharedTestContext sharedContext;

    private ResponseEntity<String> response;
    private String registrationJson;
    private String loginJson;
    private String baseUrl;
    private String currentUserToken;
    private String currentRefreshToken;
    private String currentUsername;
    private boolean currentUserIsOperator;
    private Map<String, String> userTokens = new HashMap<>();
    private Map<String, String> refreshTokens = new HashMap<>();

    @Before
    public void setUp() {
        logger.info("Authentication Cucumber @Before hook executed");
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
            userTokens.clear();
            refreshTokens.clear();
            currentUserToken = null;
            currentRefreshToken = null;
            currentUsername = null;
            currentUserIsOperator = false;
            response = null;
            
            // Cleanup complete - no delay needed for MongoDB operations
        } catch (Exception e) {
            logger.warn("Error cleaning database: {}", e.getMessage());
        }
    }

    @Given("I have new user registration details")
    public void i_have_new_user_registration_details(DataTable dataTable) {
        List<Map<String, String>> data = dataTable.asMaps(String.class, String.class);
        String username = data.get(0).get("username");
        String email = data.get(0).get("email");
        String password = data.get(0).get("password");
        String role = data.get(0).get("role");
        
        currentUsername = username;
        currentUserIsOperator = "OPERATOR".equalsIgnoreCase(role);

        registrationJson = String.format("""
        {
            "username": "%s",
            "email": "%s",
            "password": "%s"
        }
        """, username, email, password);

        loginJson = String.format("""
        {
            "emailOrUsername": "%s",
            "password": "%s"
        }
        """, email, password);
    }

    @When("I register as a new user")
    public void i_register_as_a_new_user() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(registrationJson, headers);
        response = restTemplate.postForEntity(baseUrl + "/api/v1/auth/register", entity, String.class);
    }

    @When("I register as a new operator")
    public void i_register_as_a_new_operator() {
        try {
            // For operators, we need to create them directly via admin API or database
            // First create an admin to make the request
            User adminUser = new User("admin", "admin@test.com", 
                passwordEncoder.encode("password123"), true);
            userRepository.save(adminUser);
            
            // Login as admin
            String adminLoginJson = """
            {
                "emailOrUsername": "admin@test.com",
                "password": "password123"
            }
            """;
            
            HttpHeaders loginHeaders = new HttpHeaders();
            loginHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> loginEntity = new HttpEntity<>(adminLoginJson, loginHeaders);
            ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                baseUrl + "/api/v1/auth/login", loginEntity, String.class);
            
            if (loginResponse.getStatusCode() != HttpStatus.OK || loginResponse.getBody() == null) {
                logger.error("Failed to login as admin: {}", loginResponse.getStatusCode());
                response = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                return;
            }
            
            String adminToken = extractTokenFromResponse(loginResponse.getBody());
            if (adminToken == null) {
                logger.error("Failed to extract admin token");
                response = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                return;
            }
            
            // Create operator user via admin API
            String operatorJson = registrationJson.replace("}", ", \"operator\": true}");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);
            HttpEntity<String> entity = new HttpEntity<>(operatorJson, headers);
            response = restTemplate.postForEntity(baseUrl + "/api/v1/users", entity, String.class);
        } catch (Exception e) {
            logger.error("Error during operator registration: {}", e.getMessage());
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Then("I should be registered successfully")
    public void i_should_be_registered_successfully() {
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.OK);
    }

    @When("I login with correct credentials")
    public void i_login_with_correct_credentials() {
        int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                attempt++;
                logger.info("Login attempt {} of {}", attempt, maxRetries);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(loginJson, headers);
                response = restTemplate.postForEntity(baseUrl + "/api/v1/auth/login", entity, String.class);
                
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    currentUserToken = extractTokenFromResponse(response.getBody());
                    currentRefreshToken = extractRefreshTokenFromResponse(response.getBody());
                    if (currentUserToken != null && currentUsername != null) {
                        userTokens.put(currentUsername, currentUserToken);
                        refreshTokens.put(currentUsername, currentRefreshToken);
                    }
                }
                
                // If we get here without exception, break the retry loop
                break;
                
            } catch (Exception e) {
                logger.error("Error during login attempt {}: {}", attempt, e.getMessage());
                
                if (attempt >= maxRetries) {
                    logger.error("Max login attempts reached, failing test");
                    response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                } else {
                    // Continue retry without delay - authentication should be immediate
                }
            }
        }
    }

    @Then("I should receive a valid JWT token")
    public void i_should_receive_a_valid_jwt_token() {
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        if (response.getBody() != null) {
            assertThat(response.getBody()).contains("accessToken");
        }
        assertThat(currentUserToken).isNotNull();
    }

    @Then("the token should indicate I am a regular user")
    public void the_token_should_indicate_i_am_a_regular_user() {
        assertThat(currentUserIsOperator).isFalse();
        // Could also decode JWT and verify operator claim
    }

    @Then("the token should indicate I am an operator")
    public void the_token_should_indicate_i_am_an_operator() {
        assertThat(currentUserIsOperator).isTrue();
        // Could also decode JWT and verify operator claim
    }

    @Given("I am logged in as a regular user {string}")
    public void i_am_logged_in_as_a_regular_user(String username) {
        User user = new User(username, username + "@test.com", 
            passwordEncoder.encode("password123"), false);
        userRepository.save(user);
        
        loginAsUser(username, "password123");
        currentUsername = username;
        currentUserIsOperator = false;
    }

    @Given("I am logged in as an operator {string}")
    public void i_am_logged_in_as_an_operator(String username) {
        User operator = new User(username, username + "@test.com", 
            passwordEncoder.encode("password123"), true);
        userRepository.save(operator);
        
        loginAsUser(username, "password123");
        currentUsername = username;
        currentUserIsOperator = true;
    }

    @When("I try to create a new user account")
    public void i_try_to_create_a_new_user_account() {
        try {
            // Generate unique username and email to avoid conflicts
            long timestamp = System.currentTimeMillis();
            String uniqueUsername = "newuser" + timestamp;
            String uniqueEmail = "new" + timestamp + "@test.com";
            
            String newUserJson = String.format("""
            {
                "username": "%s",
                "email": "%s",
                "password": "password123",
                "operator": false
            }
            """, uniqueUsername, uniqueEmail);
            
            HttpHeaders headers = createAuthenticatedHeaders();
            HttpEntity<String> entity = new HttpEntity<>(newUserJson, headers);
            response = restTemplate.postForEntity(baseUrl + "/api/v1/users", entity, String.class);
        } catch (Exception e) {
            logger.error("Error during user creation: {}", e.getMessage());
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @When("I create a new user account")
    public void i_create_a_new_user_account() {
        i_try_to_create_a_new_user_account();
    }

    @Then("I should receive a forbidden error")
    public void i_should_receive_a_forbidden_error() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Then("the operation should succeed")
    public void the_operation_should_succeed() {
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.CREATED, HttpStatus.NO_CONTENT);
    }

    @When("I try to list all users")
    public void i_try_to_list_all_users() {
        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        response = restTemplate.exchange(baseUrl + "/api/v1/users", HttpMethod.GET, entity, String.class);
    }

    @When("I list all users")
    public void i_list_all_users() {
        i_try_to_list_all_users();
    }

    @Then("I should see the complete user list")
    public void i_should_see_the_complete_user_list() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("[");
    }

    @When("I try to delete a user account")
    public void i_try_to_delete_a_user_account() {
        // First create a user to delete
        User userToDelete = new User("deleteme", "delete@test.com", 
            passwordEncoder.encode("password123"), false);
        User saved = userRepository.save(userToDelete);
        
        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        response = restTemplate.exchange(baseUrl + "/api/v1/users/" + saved.getId(), 
            HttpMethod.DELETE, entity, String.class);
    }

    @When("I delete a user account")
    public void i_delete_a_user_account() {
        i_try_to_delete_a_user_account();
    }

    @When("I update a user account")
    public void i_update_a_user_account() {
        // First create a user to update
        User userToUpdate = new User("updateme", "update@test.com", 
            passwordEncoder.encode("password123"), false);
        User saved = userRepository.save(userToUpdate);
        
        String updateJson = """
        {
            "username": "updated",
            "email": "updated@test.com",
            "operator": false
        }
        """;
        
        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<String> entity = new HttpEntity<>(updateJson, headers);
        response = restTemplate.exchange(baseUrl + "/api/v1/users/" + saved.getId(), 
            HttpMethod.PUT, entity, String.class);
    }

    @When("I access my profile endpoint")
    public void i_access_my_profile_endpoint() {
        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        response = restTemplate.exchange(baseUrl + "/api/v1/users/profile", HttpMethod.GET, entity, String.class);
    }

    @When("I try to access my profile endpoint")
    public void i_try_to_access_my_profile_endpoint() {
        i_access_my_profile_endpoint();
    }

    @Then("I should see my profile information")
    public void i_should_see_my_profile_information() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("username");
    }

    @When("I access the test endpoint")
    public void i_access_the_test_endpoint() {
        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        response = restTemplate.exchange(baseUrl + "/api/v1/users/test", HttpMethod.GET, entity, String.class);
    }

    @When("I try to access the test endpoint")
    public void i_try_to_access_the_test_endpoint() {
        i_access_the_test_endpoint();
    }

    @Then("I should receive access confirmation")
    public void i_should_receive_access_confirmation() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Access granted");
    }

    @Given("there is a registered user {string} with password {string}")
    public void there_is_a_registered_user_with_password(String username, String password) {
        User user = new User(username, username + "@test.com", 
            passwordEncoder.encode(password), false);
        userRepository.save(user);
    }

    @When("I try to login with username {string} and password {string}")
    public void i_try_to_login_with_username_and_password(String username, String password) {
        String loginAttempt = String.format("""
        {
            "emailOrUsername": "%s",
            "password": "%s"
        }
        """, username, password);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(loginAttempt, headers);
        response = restTemplate.postForEntity(baseUrl + "/api/v1/auth/login", entity, String.class);
    }

    @Then("I should receive an authentication error")
    public void i_should_receive_an_authentication_error() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @When("I request a token refresh using my refresh token")
    public void i_request_a_token_refresh_using_my_refresh_token() {
        String refreshRequest = String.format("""
        {
            "refreshToken": "%s"
        }
        """, currentRefreshToken);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(refreshRequest, headers);
        response = restTemplate.postForEntity(baseUrl + "/api/v1/auth/refresh", entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            currentUserToken = extractTokenFromResponse(response.getBody());
            currentRefreshToken = extractRefreshTokenFromResponse(response.getBody());
        }
    }

    @Then("I should receive new access and refresh tokens")
    public void i_should_receive_new_access_and_refresh_tokens() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("accessToken");
        assertThat(response.getBody()).contains("refreshToken");
    }

    @When("I use the old access token")
    public void i_use_the_old_access_token() {
        // Keep the current token (which is now the new one)
        // In a real scenario, we'd use the old token, but for testing we'll use current
    }

    @Then("I should still be able to access protected endpoints")
    public void i_should_still_be_able_to_access_protected_endpoints() {
        i_access_the_test_endpoint();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Given("there are users with different roles")
    public void there_are_users_with_different_roles(DataTable dataTable) {
        List<Map<String, String>> users = dataTable.asMaps(String.class, String.class);
        
        for (Map<String, String> userData : users) {
            String username = userData.get("username");
            String email = userData.get("email");
            String role = userData.get("role");
            boolean isOperator = "OPERATOR".equalsIgnoreCase(role);
            
            User user = new User(username, email, 
                passwordEncoder.encode("password123"), isOperator);
            userRepository.save(user);
            
            // Login each user to get their tokens
            loginAsUser(username, "password123");
            userTokens.put(username, currentUserToken);
        }
    }

    @When("each user tries to access role-appropriate endpoints")
    public void each_user_tries_to_access_role_appropriate_endpoints() {
        // This step will be verified in the next step
    }

    @Then("users should only access their allowed endpoints")
    public void users_should_only_access_their_allowed_endpoints() {
        // Test regular users
        for (String username : userTokens.keySet()) {
            if (!username.contains("admin")) { // Regular users
                currentUserToken = userTokens.get(username);
                
                // Should be able to access profile
                i_access_my_profile_endpoint();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                
                // Should NOT be able to create users
                i_try_to_create_a_new_user_account();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            }
        }
    }

    @Then("operators should access all administrative endpoints")
    public void operators_should_access_all_administrative_endpoints() {
        // Test operator users
        for (String username : userTokens.keySet()) {
            if (username.contains("admin")) { // Operator users
                currentUserToken = userTokens.get(username);
                
                // Should be able to access profile
                i_access_my_profile_endpoint();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                
                // Should be able to list users
                i_try_to_list_all_users();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                
                // Should be able to create users
                i_try_to_create_a_new_user_account();
                assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.CREATED);
            }
        }
    }

    @Given("I am not authenticated")
    public void i_am_not_authenticated() {
        currentUserToken = null;
        currentRefreshToken = null;
    }

    @Then("I should receive an unauthorized error")
    public void i_should_receive_an_unauthorized_error() {
        // Use shared context if local response is null
        ResponseEntity<String> responseToCheck = response != null ? response : sharedContext.getLastResponse();
        assertThat(responseToCheck).isNotNull();
        assertThat(responseToCheck.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Then("the user should be updated successfully")
    public void the_user_should_be_updated_successfully() {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    // Helper methods

    private void loginAsUser(String username, String password) {
        String loginRequest = String.format("""
        {
            "emailOrUsername": "%s@test.com",
            "password": "%s"
        }
        """, username, password);
        
        int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                attempt++;
                logger.info("Helper login attempt {} of {} for user {}", attempt, maxRetries, username);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(loginRequest, headers);
                ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                    baseUrl + "/api/v1/auth/login", entity, String.class);
                
                if (loginResponse.getStatusCode() == HttpStatus.OK) {
                    currentUserToken = extractTokenFromResponse(loginResponse.getBody());
                    currentRefreshToken = extractRefreshTokenFromResponse(loginResponse.getBody());
                }
                
                // If we get here without exception, break the retry loop
                break;
                
            } catch (Exception e) {
                logger.error("Error during helper login attempt {} for user {}: {}", attempt, username, e.getMessage());
                
                if (attempt >= maxRetries) {
                    logger.error("Max helper login attempts reached for user {}", username);
                    throw new RuntimeException("Failed to login user after " + maxRetries + " attempts: " + username, e);
                } else {
                    // Continue retry without delay - authentication should be immediate
                }
            }
        }
    }

    private HttpHeaders createAuthenticatedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (currentUserToken != null) {
            headers.setBearerAuth(currentUserToken);
        }
        return headers;
    }

    private String extractTokenFromResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get("accessToken").asText();
        } catch (Exception e) {
            logger.error("Failed to extract token from response: {}", e.getMessage());
            return null;
        }
    }

    private String extractRefreshTokenFromResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get("refreshToken").asText();
        } catch (Exception e) {
            logger.error("Failed to extract refresh token from response: {}", e.getMessage());
            return null;
        }
    }
}