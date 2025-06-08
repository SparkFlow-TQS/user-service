package tqs.sparkflow.userservice.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.restassured.RestAssured;

import tqs.sparkflow.userservice.UserServiceApplication;
import tqs.sparkflow.userservice.config.TestcontainersConfiguration;

@SpringBootTest(
    classes = {
        UserServiceApplication.class,
        TestcontainersConfiguration.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Testcontainers
class HealthControllerIT {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void whenGetHealth_thenReturnHealthy() throws Exception {
        given()
        .when()
            .get("/health")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body(equalTo("User Service is healthy"));
    }
} 