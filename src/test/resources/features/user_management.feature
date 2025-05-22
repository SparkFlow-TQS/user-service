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