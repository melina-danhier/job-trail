package com.melina.jobtrail.integration;

import com.melina.jobtrail.dto.CompanyRequest;
import com.melina.jobtrail.dto.LoginRequest;
import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.repository.CompanyRepository;
import com.melina.jobtrail.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CompanyApiIntegrationTest extends AbstractPostgresCleanIntegrationTest {

    private static final String OWNER_EMAIL = "company-owner@example.com";
    private static final String OTHER_EMAIL = "company-other@example.com";
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

    private RequestPostProcessor ownerAuth;
    private RequestPostProcessor otherAuth;

    @BeforeEach
    void setUpUsers() throws Exception {
        saveUser(OWNER_EMAIL);
        saveUser(OTHER_EMAIL);
        ownerAuth = bearerToken(login(OWNER_EMAIL, PASSWORD));
        otherAuth = bearerToken(login(OTHER_EMAIL, PASSWORD));
    }

    @Test
    @DisplayName("Company CRUD lifecycle works end-to-end against real PostgreSQL")
    void companyCrud_lifecycle_happyPath() throws Exception {
        // 1. Create company
        CompanyRequest createDto = new CompanyRequest("TechCorp GmbH", "https://techcorp.example.com", "Berlin");
        String createResponse = mockMvc.perform(json(post("/api/companies").with(ownerAuth), createDto))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").isNumber(),
                        jsonPath("$.name").value("TechCorp GmbH"),
                        jsonPath("$.website").value("https://techcorp.example.com"),
                        jsonPath("$.location").value("Berlin")
                )
                .andReturn().getResponse().getContentAsString();

        long companyId = objectMapper.readTree(createResponse).get("id").asLong();

        // 2. Get company by ID
        mockMvc.perform(get("/api/companies/{id}", companyId).with(ownerAuth))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(companyId),
                        jsonPath("$.name").value("TechCorp GmbH")
                );

        // 3. List companies
        mockMvc.perform(get("/api/companies").with(ownerAuth))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$", hasSize(1)),
                        jsonPath("$[0].id").value(companyId),
                        jsonPath("$[0].name").value("TechCorp GmbH")
                );

        // 4. Update company
        CompanyRequest updateDto = new CompanyRequest("TechCorp Global", "https://global.techcorp.example.com", "München");
        mockMvc.perform(json(put("/api/companies/{id}", companyId).with(ownerAuth), updateDto))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(companyId),
                        jsonPath("$.name").value("TechCorp Global"),
                        jsonPath("$.location").value("München")
                );

        // 5. Delete company
        mockMvc.perform(delete("/api/companies/{id}", companyId).with(ownerAuth))
                .andExpect(status().isNoContent());

        // 6. Verify 404 after deletion
        mockMvc.perform(get("/api/companies/{id}", companyId).with(ownerAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("User isolation: other user cannot view, list, update, or delete owner's company")
    void otherUser_cannotAccessOrModifyOwnerCompany() throws Exception {
        User owner = userRepository.findByEmail(OWNER_EMAIL).orElseThrow();
        Company ownerCompany = companyRepository.save(Company.builder()
                .user(owner)
                .name("Confidential AG")
                .location("Frankfurt")
                .build());
        long companyId = ownerCompany.getId();

        // Other user lists companies -> must be empty
        mockMvc.perform(get("/api/companies").with(otherAuth))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$", hasSize(0))
                );

        // Other user attempts GET
        mockMvc.perform(get("/api/companies/{id}", companyId).with(otherAuth))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404),
                        jsonPath("$.message").value("Company with id " + companyId + " not found")
                );

        // Other user attempts PUT
        CompanyRequest maliciousUpdate = new CompanyRequest("Hijacked Name", null, null);
        mockMvc.perform(json(put("/api/companies/{id}", companyId).with(otherAuth), maliciousUpdate))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404)
                );

        // Other user attempts DELETE
        mockMvc.perform(delete("/api/companies/{id}", companyId).with(otherAuth))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404)
                );

        // Verify company still exists and remains unchanged in PostgreSQL
        assertTrue(companyRepository.findById(companyId).isPresent());
    }

    @Test
    @DisplayName("Validation fails when required fields are missing or invalid")
    void companyValidation_emptyNameAndInvalidWebsite_returnsBadRequest() throws Exception {
        CompanyRequest invalid = new CompanyRequest("", "not-a-valid-url", null);

        mockMvc.perform(json(post("/api/companies").with(ownerAuth), invalid))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.message").value("Validation failed"),
                        jsonPath("$.fieldErrors.name").value("Company name cannot be null or blank"),
                        jsonPath("$.fieldErrors.website").value("Invalid website format")
                );
    }

    @Test
    @DisplayName("Querying non-existing company ID returns standard 404 ErrorResponse")
    void companyNotFound_returnsStable404() throws Exception {
        mockMvc.perform(get("/api/companies/{id}", 999_999L).with(ownerAuth))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404),
                        jsonPath("$.error").value("Not Found"),
                        jsonPath("$.message").value("Company with id 999999 not found"),
                        jsonPath("$.timestamp", matchesPattern(".+Z"))
                );
    }

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .build());
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

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }
}
