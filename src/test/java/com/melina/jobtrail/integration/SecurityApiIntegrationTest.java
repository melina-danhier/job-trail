package com.melina.jobtrail.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityApiIntegrationTest extends AbstractPostgresCleanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/applications",
            "/api/companies",
            "/api/profiles",
            "/api/auth/me"
    })
    @DisplayName("Protected GET endpoints reject unauthenticated requests with 401 Unauthorized")
    void protectedGetEndpoints_withoutToken_return401(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath("$.status").value(401),
                        jsonPath("$.error").value("Unauthorized"),
                        jsonPath("$.message").value("Unauthorized"),
                        jsonPath("$.timestamp", matchesPattern(".+Z"))
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/applications",
            "/api/companies",
            "/api/profiles",
            "/api/job-match/analyze"
    })
    @DisplayName("Protected POST endpoints reject unauthenticated requests with 401 Unauthorized")
    void protectedPostEndpoints_withoutToken_return401(String path) throws Exception {
        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath("$.status").value(401),
                        jsonPath("$.error").value("Unauthorized"),
                        jsonPath("$.message").value("Unauthorized")
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/applications/1",
            "/api/companies/1",
            "/api/profiles"
    })
    @DisplayName("Protected PUT and DELETE endpoints reject unauthenticated requests with 401")
    void protectedPutAndDeleteEndpoints_withoutToken_return401(String path) throws Exception {
        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath("$.status").value(401)
                );

        mockMvc.perform(delete(path))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath("$.status").value(401)
                );
    }

    @Test
    @DisplayName("Request with malformed or tampered JWT token returns 401 Unauthorized")
    void requestWithInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.invalidPayload.invalidSignature"))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath("$.status").value(401),
                        jsonPath("$.error").value("Unauthorized"),
                        jsonPath("$.message").value("Unauthorized")
                );
    }

    @Test
    @DisplayName("Request with expired JWT token returns 401 Unauthorized")
    void requestWithExpiredToken_returns401() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date past = new Date(now.getTime() - 60000);

        String expiredToken = Jwts.builder()
                .subject("expired-user@example.com")
                .issuer("jobtrail")
                .issuedAt(new Date(past.getTime() - 60000))
                .expiration(past)
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath("$.status").value(401),
                        jsonPath("$.error").value("Unauthorized")
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Basic dXNlcjpwYXNzd29yZA==",
            "Token some-api-key",
            "Bearer ",
            "Bearer"
    })
    @DisplayName("Request with unsupported authorization header format is rejected with 401")
    void requestWithMalformedAuthHeader_returns401(String authHeader) throws Exception {
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", authHeader))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath("$.status").value(401)
                );
    }

    @Test
    @WithMockUser(username = "guest@example.com", roles = {"GUEST"})
    @DisplayName("Authenticated user with unauthorized role receives 403 Forbidden with standard ErrorResponse")
    void userWithDisallowedRole_returns403() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpectAll(
                        status().isForbidden(),
                        jsonPath("$.status").value(403),
                        jsonPath("$.error").value("Forbidden"),
                        jsonPath("$.message").value("Access denied")
                );
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    @DisplayName("Unsupported HTTP method returns 405 Method Not Allowed with standard ErrorResponse")
    void unsupportedHttpMethod_returns405() throws Exception {
        mockMvc.perform(patch("/api/companies"))
                .andExpectAll(
                        status().isMethodNotAllowed(),
                        jsonPath("$.status").value(405),
                        jsonPath("$.error").value("Method Not Allowed"),
                        jsonPath("$.message", containsString("is not allowed for this endpoint"))
                );
    }

    @Test
    @DisplayName("Public demo login and OpenAPI documentation are accessible without authentication")
    void publicEndpoints_areAccessibleWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
