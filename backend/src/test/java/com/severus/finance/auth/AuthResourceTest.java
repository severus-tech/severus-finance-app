package com.severus.finance.auth;

import com.severus.finance.auth.dto.LoginRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class AuthResourceTest {

    @Test
    @DisplayName("Should successfully login with valid credentials")
    public void testSuccessfulLogin() {
        LoginRequest request = new LoginRequest("severus.technology@gmail.com", "1234567890");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .cookie("refresh_token", notNullValue())
                .body("accessToken", notNullValue())
                .body("email", equalTo("severus.technology@gmail.com"));
    }

    @Test
    @DisplayName("Should fail login with generic message on invalid credentials")
    public void testInvalidLogin() {
        LoginRequest request = new LoginRequest("severus.technology@gmail.com", "WrongPassword123!");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(401)
                .body("message", equalTo("Invalid email or password"))
                .body("errorCode", equalTo("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Should reject unauthorized access to protected endpoint")
    public void testProtectedEndpointRejection() {
        given()
                .when()
                .get("/api/v1/dashboard")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should allow authorized access to protected endpoint with valid JWT token")
    public void testProtectedEndpointWithValidToken() {
        LoginRequest request = new LoginRequest("severus.technology@gmail.com", "1234567890");

        String token = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("accessToken");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/dashboard")
                .then()
                .statusCode(200);
    }
}
