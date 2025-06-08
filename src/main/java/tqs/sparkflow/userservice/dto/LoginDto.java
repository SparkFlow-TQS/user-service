package tqs.sparkflow.userservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for user login requests.
 */
public class LoginDto {
  @NotBlank(message = "Email or username is required")
  private String emailOrUsername;

  @NotBlank(message = "Password is required")
  private String password;

  /**
   * Gets the email or username.
   *
   * @return the email or username
   */
  public String getEmailOrUsername() {
    return emailOrUsername;
  }

  /**
   * Sets the email or username.
   *
   * @param emailOrUsername the email or username to set
   */
  public void setEmailOrUsername(String emailOrUsername) {
    this.emailOrUsername = emailOrUsername;
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
}