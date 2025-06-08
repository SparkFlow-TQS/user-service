package tqs.sparkflow.userservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for health checks.
 * Provides endpoints for checking service health status.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

  /**
   * Performs a health check for the user service.
   *
   * @return a string indicating the service is healthy
   */
  @GetMapping
  public String healthCheck() {
    return "User Service is healthy :)";
  }
} 