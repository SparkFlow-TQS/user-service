package tqs.sparkflow.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user update requests.
 */
public class UserUpdateDto {
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  private String username;

  @Email(message = "Email should be valid")
  private String email;

  @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
  private String password;

  private Boolean operator;

  /**
   * Gets the username.
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the username.
   *
   * @param username the username to set
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Gets the email.
   *
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the email.
   *
   * @param email the email to set
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Gets the password.
   *
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the password.
   *
   * @param password the password to set
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Gets the operator status.
   *
   * @return the operator status
   */
  public Boolean getOperator() {
    return operator;
  }

  /**
   * Sets the operator status.
   *
   * @param operator the operator status to set
   */
  public void setOperator(Boolean operator) {
    this.operator = operator;
  }
}