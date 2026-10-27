package com.melina.jobtrail.security;

import com.melina.jobtrail.controller.*;
import com.melina.jobtrail.dto.CompanyRequest;
import com.melina.jobtrail.dto.CompanyResponse;
import com.melina.jobtrail.dto.UserResponse;
import com.melina.jobtrail.dto.application.ApplicationCreateRequest;
import com.melina.jobtrail.dto.application.ApplicationResponse;
import com.melina.jobtrail.dto.application.ApplicationUpdateRequest;
import com.melina.jobtrail.exception.ApplicationNotFoundException;
import com.melina.jobtrail.exception.CompanyNotFoundException;
import com.melina.jobtrail.exception.GlobalExceptionHandler;
import com.melina.jobtrail.service.*;
import com.melina.jobtrail.util.ApplicationStatus;
import com.melina.jobtrail.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        ApplicationController.class,
        CompanyController.class,
        ProfileController.class,
        AuthController.class,
        JobMatchController.class
})
@Import({
        SecurityConfig.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        GlobalExceptionHandler.class,
        JwtAuthFilter.class
})
class AccessControlSecurityWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private DemoDataService demoDataService;

    @MockitoBean
    private JobMatchAiService jobMatchAiService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Nested
    @DisplayName("1. Anonymous Access Restrictions (401 Unauthorized)")
    class AnonymousAccessTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "/api/applications",
                "/api/applications/1",
                "/api/applications/1/status-history",
                "/api/companies",
                "/api/companies/1",
                "/api/profiles",
                "/api/auth/me"
        })
        @DisplayName("Anonymous GET requests to protected endpoints return 401 with standard ErrorResponse")
        void anonymousGet_isRejectedWith401(String path) throws Exception {
            mockMvc.perform(get(path))
                    .andExpectAll(
                            status().isUnauthorized(),
                            jsonPath("$.status").value(401),
                            jsonPath("$.error").value("Unauthorized"),
                            jsonPath("$.message").value("Unauthorized"),
                            jsonPath("$.timestamp", notNullValue())
                    );
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/api/applications",
                "/api/companies",
                "/api/profiles",
                "/api/job-match/analyze"
        })
        @DisplayName("Anonymous POST requests to protected endpoints return 401 with standard ErrorResponse")
        void anonymousPost_isRejectedWith401(String path) throws Exception {
            mockMvc.perform(post(path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpectAll(
                            status().isUnauthorized(),
                            jsonPath("$.status").value(401),
                            jsonPath("$.error").value("Unauthorized")
                    );
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/api/applications/1",
                "/api/companies/1",
                "/api/profiles"
        })
        @DisplayName("Anonymous PUT requests to protected endpoints return 401")
        void anonymousPut_isRejectedWith401(String path) throws Exception {
            mockMvc.perform(put(path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpectAll(
                            status().isUnauthorized(),
                            jsonPath("$.status").value(401),
                            jsonPath("$.error").value("Unauthorized")
                    );
        }

        @Test
        @DisplayName("Anonymous PATCH request to update status returns 401")
        void anonymousPatch_isRejectedWith401() throws Exception {
            mockMvc.perform(patch("/api/applications/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"APPLIED\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/api/applications/1",
                "/api/companies/1",
                "/api/profiles"
        })
        @DisplayName("Anonymous DELETE requests to protected endpoints return 401")
        void anonymousDelete_isRejectedWith401(String path) throws Exception {
            mockMvc.perform(delete(path))
                    .andExpectAll(
                            status().isUnauthorized(),
                            jsonPath("$.status").value(401),
                            jsonPath("$.error").value("Unauthorized")
                    );
        }
    }

    @Nested
    @DisplayName("2. Missing, Invalid and Expired JWT Token Handling")
    class JwtValidationTests {

        @Test
        @DisplayName("Malformed JWT token returns 401 with standard ErrorResponse")
        void malformedToken_returns401() throws Exception {
            when(jwtUtil.validateTokenAndExtractEmail("malformed.token"))
                    .thenThrow(new MalformedJwtException("Malformed JWT"));

            mockMvc.perform(get("/api/applications")
                            .header("Authorization", "Bearer malformed.token"))
                    .andExpectAll(
                            status().isUnauthorized(),
                            jsonPath("$.status").value(401),
                            jsonPath("$.error").value("Unauthorized"),
                            jsonPath("$.message").value("Unauthorized")
                    );
        }

        @Test
        @DisplayName("Expired JWT token returns 401 with standard ErrorResponse")
        void expiredToken_returns401() throws Exception {
            when(jwtUtil.validateTokenAndExtractEmail("expired.token"))
                    .thenThrow(new ExpiredJwtException(null, null, "Expired token"));

            mockMvc.perform(get("/api/applications")
                            .header("Authorization", "Bearer expired.token"))
                    .andExpectAll(
                            status().isUnauthorized(),
                            jsonPath("$.status").value(401),
                            jsonPath("$.error").value("Unauthorized")
                    );
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Basic dXNlcjpwYXNz",
                "Token 12345",
                "Bearer ",
                "Bearer"
        })
        @DisplayName("Unsupported or invalid Authorization headers return 401")
        void invalidAuthHeader_returns401(String authHeader) throws Exception {
            mockMvc.perform(get("/api/companies")
                            .header("Authorization", authHeader))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("3. Role-Based Access Control and Disallowed Operations")
    class RoleBasedAccessTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "/api/applications",
                "/api/companies",
                "/api/profiles",
                "/api/auth/me"
        })
        @WithMockUser(username = "guest@example.com", roles = {"GUEST"})
        @DisplayName("User with disallowed role (ROLE_GUEST) receives 403 Forbidden with standard ErrorResponse")
        void disallowedRole_returns403(String path) throws Exception {
            mockMvc.perform(get(path))
                    .andExpectAll(
                            status().isForbidden(),
                            jsonPath("$.status").value(403),
                            jsonPath("$.error").value("Forbidden"),
                            jsonPath("$.message").value("Access denied"),
                            jsonPath("$.timestamp", notNullValue())
                    );
        }

        @Test
        @WithMockUser(username = "guest@example.com", roles = {"GUEST"})
        @DisplayName("POST request with disallowed role receives 403 Forbidden")
        void disallowedRolePost_returns403() throws Exception {
            mockMvc.perform(post("/api/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"positionTitle\":\"Developer\",\"companyId\":1,\"applicationDate\":\"2026-10-17\"}"))
                    .andExpectAll(
                            status().isForbidden(),
                            jsonPath("$.status").value(403),
                            jsonPath("$.error").value("Forbidden"),
                            jsonPath("$.message").value("Access denied")
                    );
        }

        @Test
        @WithMockUser(username = "norole@example.com", roles = {"NONE"})
        @DisplayName("User with non-user role receives 403 Forbidden")
        void userWithNonMatchingRole_returns403() throws Exception {
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
        void unsupportedMethod_returns405() throws Exception {
            mockMvc.perform(patch("/api/companies"))
                    .andExpectAll(
                            status().isMethodNotAllowed(),
                            jsonPath("$.status").value(405),
                            jsonPath("$.error").value("Method Not Allowed"),
                            jsonPath("$.message", containsString("is not allowed for this endpoint")),
                            jsonPath("$.timestamp", notNullValue())
                    );
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Unsupported HTTP method on application ID returns 405")
        void unsupportedMethodOnApplication_returns405() throws Exception {
            mockMvc.perform(post("/api/applications/123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpectAll(
                            status().isMethodNotAllowed(),
                            jsonPath("$.status").value(405),
                            jsonPath("$.error").value("Method Not Allowed")
                    );
        }
    }

    @Nested
    @DisplayName("4. Server-Side Identity Derivation (Ignoring Client-Supplied User ID)")
    class ServerSideIdentityDerivationTests {

        @Test
        @DisplayName("Application creation uses authenticated user context and ignores client-supplied userId query param")
        void createApplication_usesContextAndIgnoresInjectedUserIdQueryParam() throws Exception {
            CustomUserDetails authenticatedUser = new CustomUserDetails("alice@example.com", "hash");
            when(jwtUtil.validateTokenAndExtractEmail("valid.token")).thenReturn(authenticatedUser.email());
            when(userDetailsService.loadUserByUsername(authenticatedUser.email())).thenReturn(authenticatedUser);

            ApplicationResponse mockResponse = new ApplicationResponse(
                    1L,
                    new CompanyResponse(1L, "TechCorp", null, null),
                    "Backend Developer",
                    ApplicationStatus.SAVED,
                    LocalDate.of(2026, 10, 17),
                    null,
                    Instant.now(),
                    Instant.now()
            );

            when(applicationService.createApplication(eq("alice@example.com"), any(ApplicationCreateRequest.class)))
                    .thenReturn(mockResponse);

            // Attacker attempts to pass query param "?userId=999"
            mockMvc.perform(post("/api/applications?userId=999")
                            .header("Authorization", "Bearer valid.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"positionTitle\": \"Backend Developer\", \"companyId\": 1, \"applicationDate\": \"2026-10-17\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L));

            // Verify service was strictly called with authenticated email "alice@example.com", never trusting 999
            verify(applicationService).createApplication(eq("alice@example.com"), any(ApplicationCreateRequest.class));
            verify(applicationService, never()).createApplication(eq("999"), any());
        }

        @Test
        @DisplayName("Attempting to inject unknown 'userId' in JSON payload is rejected with 400 Bad Request")
        void createApplication_withInjectedJsonUserId_isRejected() throws Exception {
            CustomUserDetails authenticatedUser = new CustomUserDetails("alice@example.com", "hash");
            when(jwtUtil.validateTokenAndExtractEmail("valid.token")).thenReturn(authenticatedUser.email());
            when(userDetailsService.loadUserByUsername(authenticatedUser.email())).thenReturn(authenticatedUser);

            mockMvc.perform(post("/api/applications")
                            .header("Authorization", "Bearer valid.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"positionTitle\": \"Backend Developer\", \"companyId\": 1, \"applicationDate\": \"2026-10-17\", \"userId\": 999}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Malformed or unsupported request body"));

            verifyNoInteractions(applicationService);
        }

        @Test
        @DisplayName("GET /api/applications uses authenticated user context and ignores query param userId")
        void getApplications_usesContextAndIgnoresQueryParamUserId() throws Exception {
            CustomUserDetails authenticatedUser = new CustomUserDetails("alice@example.com", "hash");
            when(jwtUtil.validateTokenAndExtractEmail("valid.token")).thenReturn(authenticatedUser.email());
            when(userDetailsService.loadUserByUsername(authenticatedUser.email())).thenReturn(authenticatedUser);

            when(applicationService.getApplications(eq("alice@example.com"), anyInt(), anyInt(), anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(new com.melina.jobtrail.dto.PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

            mockMvc.perform(get("/api/applications?userId=999")
                            .header("Authorization", "Bearer valid.token"))
                    .andExpect(status().isOk());

            verify(applicationService).getApplications(eq("alice@example.com"), anyInt(), anyInt(), anyString(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("GET /api/companies uses authenticated user context")
        void getCompanies_usesContext() throws Exception {
            CustomUserDetails authenticatedUser = new CustomUserDetails("alice@example.com", "hash");
            when(jwtUtil.validateTokenAndExtractEmail("valid.token")).thenReturn(authenticatedUser.email());
            when(userDetailsService.loadUserByUsername(authenticatedUser.email())).thenReturn(authenticatedUser);

            when(companyService.getAllCompanies("alice@example.com")).thenReturn(List.of());

            mockMvc.perform(get("/api/companies?userId=999")
                            .header("Authorization", "Bearer valid.token"))
                    .andExpect(status().isOk());

            verify(companyService).getAllCompanies("alice@example.com");
        }

        @Test
        @DisplayName("GET /api/auth/me uses authenticated user context")
        void getMe_usesContext() throws Exception {
            CustomUserDetails authenticatedUser = new CustomUserDetails("alice@example.com", "hash");
            when(jwtUtil.validateTokenAndExtractEmail("valid.token")).thenReturn(authenticatedUser.email());
            when(userDetailsService.loadUserByUsername(authenticatedUser.email())).thenReturn(authenticatedUser);

            when(userService.getMe("alice@example.com")).thenReturn(new UserResponse("alice@example.com", Instant.now()));

            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer valid.token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("alice@example.com"));

            verify(userService).getMe("alice@example.com");
        }
    }

    @Nested
    @DisplayName("5. Access to Foreign Resources (IDOR Prevention)")
    class CrossUserAccessTests {

        @Test
        @DisplayName("Accessing another user's application returns 404 Not Found with standard ErrorResponse")
        void getForeignApplication_returns404() throws Exception {
            CustomUserDetails user = new CustomUserDetails("user@example.com", "hash");
            when(jwtUtil.validateTokenAndExtractEmail("token")).thenReturn(user.email());
            when(userDetailsService.loadUserByUsername(user.email())).thenReturn(user);

            when(applicationService.getApplicationById("user@example.com", 999L))
                    .thenThrow(new ApplicationNotFoundException(999L));

            mockMvc.perform(get("/api/applications/999")
                            .header("Authorization", "Bearer token"))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.status").value(404),
                            jsonPath("$.error").value("Not Found"),
                            jsonPath("$.message", containsString("not found")),
                            jsonPath("$.timestamp", notNullValue())
                    );
        }

        @Test
        @DisplayName("Updating another user's application returns 404 Not Found")
        void updateForeignApplication_returns404() throws Exception {
            CustomUserDetails user = new CustomUserDetails("user@example.com", "hash");
            when(jwtUtil.validateTokenAndExtractEmail("token")).thenReturn(user.email());
            when(userDetailsService.loadUserByUsername(user.email())).thenReturn(user);

            when(applicationService.updateApplication(eq("user@example.com"), eq(999L), any(ApplicationUpdateRequest.class)))
                    .thenThrow(new ApplicationNotFoundException(999L));

            mockMvc.perform(put("/api/applications/999")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"positionTitle\":\"Lead Developer\",\"companyId\":1}"))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.status").value(404),
                            jsonPath("$.error").value("Not Found")
                    );
        }

        @Test
        @DisplayName("Deleting another user's application returns 404 Not Found")
        void deleteForeignApplication_returns404() throws Exception {
            CustomUserDetails user = new CustomUserDetails("user@example.com", "hash");
            when(jwtUtil.validateTokenAndExtractEmail("token")).thenReturn(user.email());
            when(userDetailsService.loadUserByUsername(user.email())).thenReturn(user);

            doThrow(new ApplicationNotFoundException(999L))
                    .when(applicationService).deleteApplication("user@example.com", 999L);

            mockMvc.perform(delete("/api/applications/999")
                            .header("Authorization", "Bearer token"))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.status").value(404),
                            jsonPath("$.error").value("Not Found")
                    );
        }

        @Test
        @DisplayName("Accessing another user's company returns 404 Not Found")
        void getForeignCompany_returns404() throws Exception {
            CustomUserDetails user = new CustomUserDetails("user@example.com", "hash");
            when(jwtUtil.validateTokenAndExtractEmail("token")).thenReturn(user.email());
            when(userDetailsService.loadUserByUsername(user.email())).thenReturn(user);

            when(companyService.getCompanyById("user@example.com", 888L))
                    .thenThrow(new CompanyNotFoundException(888L));

            mockMvc.perform(get("/api/companies/888")
                            .header("Authorization", "Bearer token"))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.status").value(404),
                            jsonPath("$.error").value("Not Found"),
                            jsonPath("$.message", containsString("not found"))
                    );
        }

        @Test
        @DisplayName("Updating another user's company returns 404 Not Found")
        void updateForeignCompany_returns404() throws Exception {
            CustomUserDetails user = new CustomUserDetails("user@example.com", "hash");
            when(jwtUtil.validateTokenAndExtractEmail("token")).thenReturn(user.email());
            when(userDetailsService.loadUserByUsername(user.email())).thenReturn(user);

            when(companyService.updateCompany(eq("user@example.com"), eq(888L), any(CompanyRequest.class)))
                    .thenThrow(new CompanyNotFoundException(888L));

            mockMvc.perform(put("/api/companies/888")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Hacked Corp\"}"))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.status").value(404),
                            jsonPath("$.error").value("Not Found")
                    );
        }

        @Test
        @DisplayName("Deleting another user's company returns 404 Not Found")
        void deleteForeignCompany_returns404() throws Exception {
            CustomUserDetails user = new CustomUserDetails("user@example.com", "hash");
            when(jwtUtil.validateTokenAndExtractEmail("token")).thenReturn(user.email());
            when(userDetailsService.loadUserByUsername(user.email())).thenReturn(user);

            doThrow(new CompanyNotFoundException(888L))
                    .when(companyService).deleteCompany("user@example.com", 888L);

            mockMvc.perform(delete("/api/companies/888")
                            .header("Authorization", "Bearer token"))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.status").value(404),
                            jsonPath("$.error").value("Not Found")
                    );
        }

        @Test
        @DisplayName("Creating application linked to another user's company returns 404 Not Found")
        void createApplication_withForeignCompany_returns404() throws Exception {
            CustomUserDetails user = new CustomUserDetails("user@example.com", "hash");
            when(jwtUtil.validateTokenAndExtractEmail("token")).thenReturn(user.email());
            when(userDetailsService.loadUserByUsername(user.email())).thenReturn(user);

            when(applicationService.createApplication(eq("user@example.com"), any(ApplicationCreateRequest.class)))
                    .thenThrow(new CompanyNotFoundException(888L));

            mockMvc.perform(post("/api/applications")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"positionTitle\":\"Developer\",\"companyId\":888,\"applicationDate\":\"2026-10-17\"}"))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.status").value(404),
                            jsonPath("$.error").value("Not Found"),
                            jsonPath("$.message", containsString("not found"))
                    );
        }
    }
}
