Feature: User Management
  As a system administrator
  I want to manage users
  So that I can control access to the system

  Scenario: Create a new user
    Given I am an authenticated administrator
    When I create a new user with the following details
      | username | email           | role    |
      | johndoe  | john@email.com  | USER    |
    Then the user should be created successfully
    And I should receive a confirmation message

  Scenario: Update user role
    Given I am an authenticated administrator
    And there is an existing user "johndoe"
    When I update the user's role to "ADMIN"
    Then the user's role should be updated successfully
    And I should receive a confirmation message

  Scenario: Delete user
    Given I am an authenticated administrator
    And there is an existing user "johndoe"
    When I delete the user
    Then the user should be deleted successfully
    And I should receive a confirmation message

  Scenario: Unauthorized access attempt
    Given I am not authenticated
    When I try to create a new user
    Then I should receive an unauthorized error
    And no user should be created

  Scenario: Create user with duplicate email
    Given I am an authenticated administrator
    And there is an existing user with email "john@email.com"
    When I create a new user with the following details
      | username | email           | role    |
      | newuser  | john@email.com  | USER    |
    Then I should receive a conflict error
    And the user should not be created

  Scenario: Get user profile information
    Given I am an authenticated user "johndoe"
    When I request my profile information
    Then I should receive my profile details
    And the response should contain my username and email

  Scenario: Update user with invalid data
    Given I am an authenticated administrator
    And there is an existing user "johndoe"
    When I try to update the user with invalid email format "invalid-email"
    Then I should receive a validation error
    And the user should not be updated

  Scenario: Access test endpoint as authenticated user
    Given I am an authenticated user "johndoe"
    When I access the protected test endpoint
    Then I should receive a success response
    And the response should confirm my access

  Scenario: List all users as administrator
    Given I am an authenticated administrator
    And there are multiple users in the system
    When I request the list of all users
    Then I should receive a list containing all users
    And each user should have username and email information 