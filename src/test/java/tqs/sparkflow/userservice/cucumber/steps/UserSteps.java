package tqs.sparkflow.userservice.cucumber.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.datatable.DataTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.core.ParameterizedTypeReference;
import io.cucumber.java.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

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

    private ResponseEntity<String> response;
    private String userJson;
    private String lastCreatedUsername;
    private String baseUrl;

    @Before
    public void setUp() {
        logger.info("Cucumber @Before hook executed");
        String port = environment.getProperty("local.server.port", "8080");
        baseUrl = "http://localhost:" + port;
    }

    @Before
    public void cleanDatabase() {
        mongoTemplate.dropCollection("users");
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
        // Verify we can access the API
        try {
            ResponseEntity<String> testResponse = restTemplate
                .withBasicAuth("test", "test")
                .getForEntity(baseUrl + "/api/v1/users", String.class);
            logger.info("Authentication test response - Status: {}, Headers: {}", 
                testResponse.getStatusCode(), testResponse.getHeaders());
            
            if (testResponse.getStatusCode() != HttpStatus.OK) {
                logger.error("Failed to authenticate as admin. Status: {}, Body: {}, Headers: {}", 
                    testResponse.getStatusCode(), testResponse.getBody(), testResponse.getHeaders());
                throw new RuntimeException("Failed to authenticate as admin. Status: " + testResponse.getStatusCode());
            }
            logger.info("Successfully authenticated as admin");
        } catch (Exception e) {
            logger.error("Error during authentication test: {}", e.getMessage(), e);
            throw new RuntimeException("Error during authentication test: " + e.getMessage(), e);
        }
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(userJson, headers);
        response = restTemplate
            .withBasicAuth("test", "test")
            .postForEntity(baseUrl + "/api/v1/users", entity, String.class);
        
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
        ResponseEntity<String> deleteResponse = restTemplate
            .withBasicAuth("test", "test")
            .exchange(baseUrl + "/api/v1/users/" + id, HttpMethod.DELETE, null, String.class);

        logger.info("Delete response status: {}", deleteResponse.getStatusCode());
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        // Store the response in the class field
        this.response = deleteResponse;
    }

    @Then("the user should be deleted successfully")
    public void the_user_should_be_deleted_successfully() {
        logger.info("Asserting delete response: {}", response);
        assertThat(response)
            .withFailMessage("Response is null! Did you forget to call a step that sets it? Scenario: [add scenario name here if possible]")
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(updateJson, headers);
        response = restTemplate
            .withBasicAuth("test", "test")
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(userJson, headers);
        response = restTemplate
            .withBasicAuth("test", "test")
            .postForEntity(baseUrl + "/api/v1/users", entity, String.class);
        
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

        ResponseEntity<Void> deleteResponse = restTemplate
            .withBasicAuth("test", "test")
            .exchange(baseUrl + "/api/v1/users/" + userId, HttpMethod.DELETE, null, Void.class);

        logger.info("Delete response status: {}", deleteResponse.getStatusCode());
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Store the response in the class field
        this.response = ResponseEntity.status(deleteResponse.getStatusCode()).build();
    }

    private String getUserIdByUsername(String username) {
        try {
            ResponseEntity<List<Map<String, Object>>> usersResponse = restTemplate
                .withBasicAuth("test", "test")
                .exchange(baseUrl + "/api/v1/users", HttpMethod.GET, null, 
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
}
