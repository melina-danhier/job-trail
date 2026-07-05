package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.LoginRequest;
import com.melina.jobtrail.dto.application.ApplicationRequest;
import com.melina.jobtrail.dto.application.ApplicationUpdateStatusRequest;
import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.repository.CompanyRepository;
import com.melina.jobtrail.repository.UserRepository;
import com.melina.jobtrail.util.ApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@Sql(
        statements = {
                "DELETE FROM application_status_history",
                "DELETE FROM applications",
                "DELETE FROM companies",
                "DELETE FROM users"
        },
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class ApplicationApiIntegrationTest {

    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String OTHER_EMAIL = "other@example.com";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Company ownerCompany;
    private RequestPostProcessor ownerAuthentication;
    private RequestPostProcessor otherAuthentication;

    @BeforeEach
    void prepareAuthenticatedRequests() throws Exception {
        User owner = saveUser(OWNER_EMAIL);
        saveUser(OTHER_EMAIL);
        ownerCompany = companyRepository.save(Company.builder().user(owner).name("Acme").build());

        ownerAuthentication = bearerToken(login(OWNER_EMAIL, PASSWORD));
        otherAuthentication = bearerToken(login(OTHER_EMAIL, PASSWORD));
    }

    @Test
    void publicApplicationApi_happyPaths_areCovered() throws Exception {
        long id = createApplication("Backend Developer");

        mockMvc.perform(authenticated(get("/api/applications"), ownerAuthentication))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.content[0].id").value(id),
                        jsonPath("$.content[0].positionTitle").value("Backend Developer")
                );

        mockMvc.perform(authenticated(get("/api/applications/{id}", id), ownerAuthentication))
                .andExpectAll(status().isOk(), jsonPath("$.id").value(id));

        ApplicationRequest replacement = new ApplicationRequest(
                "Senior Backend Developer", ownerCompany.getId(), ApplicationStatus.APPLIED, null, null
        );
        mockMvc.perform(json(authenticated(put("/api/applications/{id}", id), ownerAuthentication), replacement))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.positionTitle").value("Senior Backend Developer"),
                        jsonPath("$.status").value("APPLIED")
                );

        ApplicationUpdateStatusRequest statusUpdate =
                new ApplicationUpdateStatusRequest(ApplicationStatus.INTERVIEW_SCHEDULED);
        mockMvc.perform(json(
                        authenticated(patch("/api/applications/{id}/status", id), ownerAuthentication),
                        statusUpdate
                ))
                .andExpectAll(status().isOk(), jsonPath("$.status").value("INTERVIEW_SCHEDULED"));

        mockMvc.perform(authenticated(get("/api/applications/{id}/status-history", id), ownerAuthentication))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$[0].newStatus").value("SAVED"),
                        jsonPath("$[1].previousStatus").value("APPLIED"),
                        jsonPath("$[1].newStatus").value("INTERVIEW_SCHEDULED")
                );

        mockMvc.perform(authenticated(delete("/api/applications/{id}", id), ownerAuthentication))
                .andExpect(status().isNoContent());

        mockMvc.perform(authenticated(get("/api/applications/{id}", id), ownerAuthentication))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedRequest_withoutCredentials_returnsStructuredUnauthorized() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath("$.status").value(401),
                        jsonPath("$.error").value("Unauthorized"),
                        jsonPath("$.message").value("Unauthorized"),
                        jsonPath("$.timestamp", matchesPattern(".+Z"))
                );
    }

    @Test
    void login_withWrongPassword_returnsStructuredUnauthorized() throws Exception {
        mockMvc.perform(json(post("/api/auth/login"), new LoginRequest(OWNER_EMAIL, "wrong-password")))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath("$.status").value(401),
                        jsonPath("$.error").value("Unauthorized"),
                        jsonPath("$.message").value("Invalid credentials")
                );
    }

    @Test
    void otherUser_cannotAccessApplicationOrItsHistory() throws Exception {
        long id = createApplication("Private application");
        mockMvc.perform(json(
                        authenticated(patch("/api/applications/{id}/status", id), ownerAuthentication),
                        new ApplicationUpdateStatusRequest(ApplicationStatus.APPLIED)
                ))
                .andExpect(status().isOk());

        assertNotFound(authenticated(get("/api/applications/{id}", id), otherAuthentication), id);
        assertNotFound(
                authenticated(get("/api/applications/{id}/status-history", id), otherAuthentication), id
        );
    }

    @Test
    void validationAndNotFoundErrors_haveStableFormat() throws Exception {
        ApplicationRequest invalid = new ApplicationRequest("", null, null, null, null);
        mockMvc.perform(json(authenticated(post("/api/applications"), ownerAuthentication), invalid))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.error").value("Bad Request"),
                        jsonPath("$.message").value("Validation failed"),
                        jsonPath("$.fieldErrors.positionTitle")
                                .value("Position title cannot be null or blank"),
                        jsonPath("$.fieldErrors.companyId").value("Company ID cannot be null"),
                        jsonPath("$.timestamp", matchesPattern(".+Z"))
                );

        assertNotFound(authenticated(get("/api/applications/{id}", 999_999L), ownerAuthentication), 999_999L);
    }

    @Test
    void sameUser_cannotCreateSameCompanyAndPositionTwice() throws Exception {
        createApplication("Backend Developer");

        ApplicationRequest duplicate = new ApplicationRequest(
                "Backend Developer", ownerCompany.getId(), ApplicationStatus.SAVED, null, null
        );
        mockMvc.perform(json(authenticated(post("/api/applications"), ownerAuthentication), duplicate))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath("$.status").value(409),
                        jsonPath("$.error").value("Conflict"),
                        jsonPath("$.message")
                                .value("An application for this company and position already exists"),
                        jsonPath("$.timestamp", matchesPattern(".+Z"))
                );
    }

    @Test
    void differentUser_canCreateSameCompanyNameAndPosition() throws Exception {
        createApplication("Backend Developer");
        User other = userRepository.findByEmail(OTHER_EMAIL).orElseThrow();
        Company otherCompany = companyRepository.save(Company.builder().user(other).name("Acme").build());
        ApplicationRequest request = new ApplicationRequest(
                "Backend Developer", otherCompany.getId(), ApplicationStatus.SAVED, null, null
        );

        mockMvc.perform(json(authenticated(post("/api/applications"), otherAuthentication), request))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.positionTitle").value("Backend Developer"),
                        jsonPath("$.company.name").value("Acme")
                );
    }

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .build());
    }

    private long createApplication(String positionTitle) throws Exception {
        ApplicationRequest request = new ApplicationRequest(
                positionTitle, ownerCompany.getId(), ApplicationStatus.SAVED, null, null
        );
        String body = mockMvc.perform(json(
                        authenticated(post("/api/applications"), ownerAuthentication), request
                ))
                .andExpectAll(status().isCreated(), jsonPath("$.positionTitle").value(positionTitle))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private String login(String email, String password) throws Exception {
        return mockMvc.perform(json(post("/api/auth/login"), new LoginRequest(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private RequestPostProcessor bearerToken(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    private MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder request,
            RequestPostProcessor authentication
    ) {
        return request.with(authentication);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    private void assertNotFound(MockHttpServletRequestBuilder request, long id) throws Exception {
        mockMvc.perform(request)
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404),
                        jsonPath("$.error").value("Not Found"),
                        jsonPath("$.message").value("Application with id " + id + " not found"),
                        jsonPath("$.timestamp", matchesPattern(".+Z"))
                );
    }
}
