package com.melina.jobtrail.integration;

import com.melina.jobtrail.dto.LoginRequest;
import com.melina.jobtrail.dto.profile.ProfileLanguageRequest;
import com.melina.jobtrail.dto.profile.ProfileProjectRequest;
import com.melina.jobtrail.dto.profile.ProfileRequest;
import com.melina.jobtrail.dto.profile.ProfileSkillRequest;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.repository.UserRepository;
import com.melina.jobtrail.util.ExperienceLevel;
import com.melina.jobtrail.util.LanguageLevel;
import com.melina.jobtrail.util.SkillLevel;
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

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileApiIntegrationTest extends AbstractPostgresCleanIntegrationTest {

    private static final String USER1_EMAIL = "profile-user1@example.com";
    private static final String USER2_EMAIL = "profile-user2@example.com";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RequestPostProcessor user1Auth;
    private RequestPostProcessor user2Auth;

    @BeforeEach
    void setUpUsers() throws Exception {
        saveUser(USER1_EMAIL);
        saveUser(USER2_EMAIL);
        user1Auth = bearerToken(login(USER1_EMAIL, PASSWORD));
        user2Auth = bearerToken(login(USER2_EMAIL, PASSWORD));
    }

    @Test
    @DisplayName("Full profile lifecycle: nested entities cascade properly to Postgres, orphan removal works, and delete cleans up")
    void profileCrud_andPostgresCascadePersistence_happyPath() throws Exception {
        // 1. Create Profile with all nested relations (skills, languages, projects, collection tables)
        ProfileRequest createRequest = new ProfileRequest(
                "Senior Backend Developer",
                "Berlin (Remote)",
                "Immediately",
                ExperienceLevel.SENIOR,
                "Passionate Java engineer with microservices expertise.",
                List.of(
                        new ProfileSkillRequest("Java", SkillLevel.EXPERT, true),
                        new ProfileSkillRequest("PostgreSQL", SkillLevel.PROFICIENT, false)
                ),
                List.of(
                        new ProfileLanguageRequest("German", LanguageLevel.NATIVE),
                        new ProfileLanguageRequest("English", LanguageLevel.FLUENT)
                ),
                List.of(
                        new ProfileProjectRequest("JobTrail", "Application tracking system", Set.of("Java", "Spring Boot", "Docker"))
                ),
                Set.of("Senior Backend Developer", "Java Architect"),
                Set.of("PHP", "COBOL")
        );

        mockMvc.perform(json(post("/api/profiles").with(user1Auth), createRequest))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").isNumber(),
                        jsonPath("$.targetRole").value("Senior Backend Developer"),
                        jsonPath("$.locationPreference").value("Berlin (Remote)"),
                        jsonPath("$.experienceLevel").value("SENIOR"),
                        jsonPath("$.skills", hasSize(2)),
                        jsonPath("$.skills[0].name").value("Java"),
                        jsonPath("$.languages", hasSize(2)),
                        jsonPath("$.projects", hasSize(1)),
                        jsonPath("$.projects[0].technologies", hasSize(3)),
                        jsonPath("$.preferredRoles", hasItem("Java Architect")),
                        jsonPath("$.avoidKeywords", hasItem("PHP"))
                );

        // Verify entity persisted in PostgreSQL through user relationship
        User persistedUser = userRepository.findByEmail(USER1_EMAIL).orElseThrow();
        assertNotNull(persistedUser.getProfile());
        assertNotNull(persistedUser.getProfile().getId());

        // 2. GET Profile
        mockMvc.perform(get("/api/profiles").with(user1Auth))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.targetRole").value("Senior Backend Developer"),
                        jsonPath("$.skills", hasSize(2)),
                        jsonPath("$.languages", hasSize(2)),
                        jsonPath("$.projects", hasSize(1))
                );

        // 3. PUT Profile (Update with orphan removal: replace skills and clear projects)
        ProfileRequest updateRequest = new ProfileRequest(
                "Staff Backend Engineer",
                "München",
                "In 3 months",
                ExperienceLevel.SENIOR,
                "Updated summary",
                List.of(new ProfileSkillRequest("Kotlin", SkillLevel.EXPERT, true)),
                List.of(new ProfileLanguageRequest("German", LanguageLevel.NATIVE)),
                List.of(),
                Set.of("Staff Engineer"),
                Set.of("PHP")
        );

        mockMvc.perform(json(put("/api/profiles").with(user1Auth), updateRequest))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.targetRole").value("Staff Backend Engineer"),
                        jsonPath("$.experienceLevel").value("SENIOR"),
                        jsonPath("$.skills", hasSize(1)),
                        jsonPath("$.skills[0].name").value("Kotlin"),
                        jsonPath("$.projects", hasSize(0))
                );

        // 4. DELETE Profile
        mockMvc.perform(delete("/api/profiles").with(user1Auth))
                .andExpect(status().isNoContent());

        // Verify GET now returns 404
        mockMvc.perform(get("/api/profiles").with(user1Auth))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404),
                        jsonPath("$.message").value("Profile not found")
                );

        // Verify user still exists in DB but profile reference is null
        User userAfterDelete = userRepository.findByEmail(USER1_EMAIL).orElseThrow();
        assertNull(userAfterDelete.getProfile());
    }

    @Test
    @DisplayName("User isolation: User 2 cannot access or see User 1's profile")
    void otherUser_hasIndependentProfile_isolation() throws Exception {
        ProfileRequest request1 = new ProfileRequest(
                "Role User 1", "Berlin", "Now", ExperienceLevel.MID,
                "Summary 1", List.of(), List.of(), List.of(), Set.of(), Set.of()
        );
        mockMvc.perform(json(post("/api/profiles").with(user1Auth), request1))
                .andExpect(status().isCreated());

        // User 2 has no profile yet -> returns 404
        mockMvc.perform(get("/api/profiles").with(user2Auth))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.message").value("Profile not found")
                );

        // User 2 creates own profile
        ProfileRequest request2 = new ProfileRequest(
                "Role User 2", "Hamburg", "1 month", ExperienceLevel.JUNIOR,
                "Summary 2", List.of(), List.of(), List.of(), Set.of(), Set.of()
        );
        mockMvc.perform(json(post("/api/profiles").with(user2Auth), request2))
                .andExpect(status().isCreated());

        // Each user receives their own profile
        mockMvc.perform(get("/api/profiles").with(user1Auth))
                .andExpectAll(status().isOk(), jsonPath("$.targetRole").value("Role User 1"));
        mockMvc.perform(get("/api/profiles").with(user2Auth))
                .andExpectAll(status().isOk(), jsonPath("$.targetRole").value("Role User 2"));
    }

    @Test
    @DisplayName("Creating profile when user already has one returns 409 Conflict")
    void createProfile_whenProfileAlreadyExists_returnsConflict() throws Exception {
        ProfileRequest request = new ProfileRequest(
                "Backend Dev", "Berlin", "Now", ExperienceLevel.MID,
                "Summary", List.of(), List.of(), List.of(), Set.of(), Set.of()
        );

        mockMvc.perform(json(post("/api/profiles").with(user1Auth), request))
                .andExpect(status().isCreated());

        // Second create attempt -> 409 Conflict
        mockMvc.perform(json(post("/api/profiles").with(user1Auth), request))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath("$.status").value(409),
                        jsonPath("$.message").value("Profile already exists")
                );
    }

    @Test
    @DisplayName("GET, PUT and DELETE without existing profile return 404 Not Found")
    void missingProfile_returnsNotFound() throws Exception {
        ProfileRequest dummy = new ProfileRequest(
                "Role", "City", "Now", ExperienceLevel.JUNIOR,
                "Summary", List.of(), List.of(), List.of(), Set.of(), Set.of()
        );

        mockMvc.perform(get("/api/profiles").with(user1Auth))
                .andExpectAll(status().isNotFound(), jsonPath("$.message").value("Profile not found"));

        mockMvc.perform(json(put("/api/profiles").with(user1Auth), dummy))
                .andExpectAll(status().isNotFound(), jsonPath("$.message").value("Profile not found"));

        mockMvc.perform(delete("/api/profiles").with(user1Auth))
                .andExpectAll(status().isNotFound(), jsonPath("$.message").value("Profile not found"));
    }

    @Test
    @DisplayName("Invalid profile payload returns 400 Bad Request with field errors")
    void createProfile_withInvalidNestedData_returnsBadRequest() throws Exception {
        // Blank targetRole and invalid nested skill
        String invalidJson = """
                {
                  "targetRole": "",
                  "locationPreference": "Berlin",
                  "availability": "Sofort",
                  "experienceLevel": "SENIOR",
                  "skills": [
                    { "name": "", "level": "EXPERT", "mainSkill": true }
                  ]
                }
                """;

        mockMvc.perform(post("/api/profiles").with(user1Auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.message").value("Validation failed")
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
