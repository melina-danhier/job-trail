package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.profile.ProfileLanguageRequest;
import com.melina.jobtrail.dto.profile.ProfileProjectRequest;
import com.melina.jobtrail.dto.profile.ProfileRequest;
import com.melina.jobtrail.dto.profile.ProfileResponse;
import com.melina.jobtrail.dto.profile.ProfileSkillRequest;
import com.melina.jobtrail.exception.ProfileAlreadyExistsException;
import com.melina.jobtrail.exception.ProfileNotFoundException;
import com.melina.jobtrail.service.ProfileService;
import com.melina.jobtrail.util.ExperienceLevel;
import com.melina.jobtrail.util.SkillLevel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest extends AbstractControllerTest {

    @MockitoBean private ProfileService profileService;

    @Test
    void createProfile_returnsCreated() throws Exception {
        ProfileRequest request = validRequest();
        when(profileService.createProfile(eq("user@example.com"), any())).thenReturn(response());

        mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(status().isCreated(), jsonPath("$.id").value(1),
                        jsonPath("$.targetRole").value("Backend Developer"));
    }

    @Test
    void createProfile_withMissingRequiredFields_returnsBadRequestAndFieldErrors() throws Exception {
        String json = "{}";

        mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.error").value("Bad Request"),
                        jsonPath("$.message").value("Validation failed"),
                        jsonPath("$.fieldErrors.targetRole").value("Target role cannot be null or blank"),
                        jsonPath("$.fieldErrors.locationPreference").value("Location preference cannot be null or blank"),
                        jsonPath("$.fieldErrors.availability").value("Availability cannot be null or blank"),
                        jsonPath("$.fieldErrors.experienceLevel").value("Experience level cannot be null")
                );
        verifyNoInteractions(profileService);
    }

    @Test
    void createProfile_withInvalidNestedSkill_returnsBadRequest() throws Exception {
        String json = """
                {"targetRole":"Backend Developer","locationPreference":"Berlin",
                 "availability":"Immediately","experienceLevel":"MID",
                 "skills":[{"name":"","level":"EXPERT","mainSkill":true}]}
                """;

        mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.message").value("Validation failed"),
                        jsonPath("$.fieldErrors['skills[0].name']").value("Skill name cannot be null or blank")
                );
        verifyNoInteractions(profileService);
    }

    @Test
    void createProfile_withInvalidNestedLanguage_returnsBadRequest() throws Exception {
        String json = """
                {"targetRole":"Backend Developer","locationPreference":"Berlin",
                 "availability":"Immediately","experienceLevel":"MID",
                 "languages":[{"language":"","level":"FLUENT"}]}
                """;

        mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.message").value("Validation failed"),
                        jsonPath("$.fieldErrors['languages[0].language']").value("Language name cannot be null or blank")
                );
        verifyNoInteractions(profileService);
    }

    @Test
    void createProfile_withInvalidNestedProject_returnsBadRequest() throws Exception {
        String json = """
                {"targetRole":"Backend Developer","locationPreference":"Berlin",
                 "availability":"Immediately","experienceLevel":"MID",
                 "projects":[{"name":"","description":"Short"}]}
                """;

        mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.message").value("Validation failed"),
                        jsonPath("$.fieldErrors['projects[0].name']").value("Project name cannot be null or blank")
                );
        verifyNoInteractions(profileService);
    }

    @Test
    void createProfile_whenProfileAlreadyExists_returnsConflict() throws Exception {
        when(profileService.createProfile(eq("user@example.com"), any()))
                .thenThrow(new ProfileAlreadyExistsException());

        mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath("$.status").value(409),
                        jsonPath("$.error").value("Conflict"),
                        jsonPath("$.message").value("Profile already exists")
                );
    }

    @Test
    void getMissingProfile_returnsNotFound() throws Exception {
        when(profileService.getProfile("user@example.com")).thenThrow(new ProfileNotFoundException());

        mockMvc.perform(get("/api/profiles"))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404),
                        jsonPath("$.error").value("Not Found"),
                        jsonPath("$.message").value("Profile not found")
                );
    }

    @Test
    void updateProfile_returnsUpdatedProfile() throws Exception {
        when(profileService.updateProfile(eq("user@example.com"), any())).thenReturn(response());

        mockMvc.perform(put("/api/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpectAll(status().isOk(), jsonPath("$.targetRole").value("Backend Developer"));
    }

    @Test
    void updateProfile_whenProfileDoesNotExist_returnsNotFound() throws Exception {
        when(profileService.updateProfile(eq("user@example.com"), any()))
                .thenThrow(new ProfileNotFoundException());

        mockMvc.perform(put("/api/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404),
                        jsonPath("$.error").value("Not Found"),
                        jsonPath("$.message").value("Profile not found")
                );
    }

    @Test
    void deleteProfile_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/profiles")).andExpect(status().isNoContent());
        verify(profileService).deleteProfile("user@example.com");
    }

    @Test
    void deleteProfile_whenProfileDoesNotExist_returnsNotFound() throws Exception {
        doThrow(new ProfileNotFoundException()).when(profileService).deleteProfile("user@example.com");

        mockMvc.perform(delete("/api/profiles"))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404),
                        jsonPath("$.error").value("Not Found"),
                        jsonPath("$.message").value("Profile not found")
                );
    }

    private ProfileRequest validRequest() {
        return new ProfileRequest("Backend Developer", "Berlin", "Immediately", ExperienceLevel.MID,
                "Java developer", List.of(), List.of(), List.of(), Set.of("Backend"), Set.of("PHP"));
    }

    private ProfileResponse response() {
        return new ProfileResponse(1L, "Backend Developer", "Berlin", "Immediately", ExperienceLevel.MID,
                "Java developer", List.of(), List.of(), List.of(), Set.of("Backend"), Set.of("PHP"));
    }
}
