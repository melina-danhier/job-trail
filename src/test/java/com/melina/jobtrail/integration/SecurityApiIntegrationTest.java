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
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("Protected POST endpoint /api/job-match/analyze rejects unauthenticated requests with 401")
    void protectedJobMatchEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/job-match/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Senior Java Developer\"}"))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath("$.status").value(401),
                        jsonPath("$.error").value("Unauthorized")
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
    @DisplayName("Public demo login and OpenAPI documentation are accessible without authentication")
    void publicEndpoints_areAccessibleWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
