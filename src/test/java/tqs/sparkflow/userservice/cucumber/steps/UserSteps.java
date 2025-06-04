package tqs.sparkflow.userservice.cucumber.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

public class UserSteps {
    @Given("I have user data")
    public void i_have_user_data() {
        // setup user data
    }

    @When("I send a POST request to /api/v1/users")
    public void i_send_post_request() {
        // send request
    }

    @Then("the response status should be 201")
    public void the_response_status_should_be_201() {
        // assert response
    }
}
