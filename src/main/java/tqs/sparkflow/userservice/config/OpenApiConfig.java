package tqs.sparkflow.userservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAPI documentation.
 * Defines the API information, contact details, and license.
 */
@Configuration
public class OpenApiConfig {

  /**
   * Creates and configures the OpenAPI documentation for the User Service.
   *
   * @return configured OpenAPI instance with API information
   */
  @Bean
  public OpenAPI configureOpenApi() {
    return new OpenAPI()
      .info(new Info()
        .title("User Service API")
        .version("0.0.1")
        .description("API for managing users with JWT authentication")
      )
      .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
      .components(new Components()
        .addSecuritySchemes("bearerAuth", new SecurityScheme()
          .name("bearerAuth")
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")
        )
      );
  }
} 