package tqs.sparkflow.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for JWT authentication responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponseDto {
  private String accessToken;
  private String refreshToken;
  private String tokenType = "Bearer";
  private String username;
  private String email;
  private boolean isOperator;

  /**
   * Constructor for JWT response with authentication details.
   *
   * @param accessToken the access token
   * @param refreshToken the refresh token
   * @param username the username
   * @param email the email
   * @param isOperator whether the user is an operator
   */
  public JwtResponseDto(String accessToken, String refreshToken, String username, 
                        String email, boolean isOperator) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.username = username;
    this.email = email;
    this.isOperator = isOperator;
  }
}