package tqs.sparkflow.userservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
        
  @Id
  private String id;

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  @Indexed(unique = true)
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 6, message = "Password must be at least 6 characters")
  private String password;

  @Field("is_operator")
  private boolean isOperator;
   
  public User(String username, String email, String password) {
    this.username = username;
    this.email = email;
    this.password = password;
    this.isOperator = false;
  }
   
  public User(String username, String email, String password, boolean isOperator) {
    this.username = username;
    this.email = email;
    this.password = password;
    this.isOperator = isOperator;
  }
} 