Feature: Authentication and Authorization
  As a system administrator and user
  I want to test authentication and role-based authorization
  So that the system properly controls access based on user roles

  Scenario: User registration and login flow
    Given I have new user registration details
      | username | email             | password    | role |
      | newuser  | new@example.com   | password123 | USER |
    When I register as a new user
    Then I should be registered successfully
    When I login with correct credentials
    Then I should receive a valid JWT token
    And the token should indicate I am a regular user

  Scenario: Operator registration and elevated access
    Given I have new user registration details
      | username | email                | password    | role     |
      | operator | operator@example.com | password123 | OPERATOR |
    When I register as a new operator
    Then I should be registered successfully
    When I login with correct credentials
    Then I should receive a valid JWT token
    And the token should indicate I am an operator

  Scenario: User tries to access operator-only endpoints
    Given I am logged in as a regular user "normaluser"
    When I try to create a new user account
    Then I should receive a forbidden error
    When I try to list all users
    Then I should receive a forbidden error
    When I try to delete a user account
    Then I should receive a forbidden error

  Scenario: Operator can access all administrative endpoints
    Given I am logged in as an operator "adminuser"
    When I create a new user account
    Then the operation should succeed
    When I list all users
    Then I should see the complete user list
    When I update a user account
    Then the user should be updated successfully
    When I delete a user account
    Then the operation should succeed

  Scenario: Both users and operators can access their own profile
    Given I am logged in as a regular user "profileuser"
    When I access my profile endpoint
    Then I should see my profile information
    When I access the test endpoint
    Then I should receive access confirmation

  Scenario: Both users and operators can access their own profile as operator
    Given I am logged in as an operator "profileoperator"
    When I access my profile endpoint
    Then I should see my profile information
    When I access the test endpoint
    Then I should receive access confirmation

  Scenario: Invalid login attempts
    Given there is a registered user "testuser" with password "correctpass"
    When I try to login with username "testuser" and password "wrongpass"
    Then I should receive an authentication error
    When I try to login with username "nonexistent" and password "anypass"
    Then I should receive an authentication error

  Scenario: Token refresh functionality
    Given I am logged in as a regular user "refreshuser"
    When I request a token refresh using my refresh token
    Then I should receive new access and refresh tokens
    When I use the old access token
    Then I should still be able to access protected endpoints

  Scenario: Access without authentication
    Given I am not authenticated
    When I try to access my profile endpoint
    Then I should receive an unauthorized error
    When I try to access the test endpoint
    Then I should receive an unauthorized error
    When I try to list all users
    Then I should receive an unauthorized error

  Scenario: Role-based endpoint access validation
    Given there are users with different roles
      | username   | email               | role     |
      | regular1   | regular1@test.com   | USER     |
      | regular2   | regular2@test.com   | USER     |
      | admin1     | admin1@test.com     | OPERATOR |
      | admin2     | admin2@test.com     | OPERATOR |
    When each user tries to access role-appropriate endpoints
    Then users should only access their allowed endpoints
    And operators should access all administrative endpoints