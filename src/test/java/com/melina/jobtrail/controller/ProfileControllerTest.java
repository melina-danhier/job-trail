package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.profile.ProfileRequest;
import com.melina.jobtrail.dto.profile.ProfileResponse;
import com.melina.jobtrail.exception.ProfileNotFoundException;
import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.security.JwtAuthFilter;
import com.melina.jobtrail.service.ProfileService;
import com.melina.jobtrail.util.ExperienceLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private ProfileService profileService;
    @MockitoBean private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void authenticate() {
        CustomUserDetails details = new CustomUserDetails("user@example.com", "hash");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

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
    void createProfile_withInvalidNestedSkill_returnsBadRequest() throws Exception {
        String json = """
                {"targetRole":"Backend Developer","locationPreference":"Berlin",
                 "availability":"Immediately","experienceLevel":"MID",
                 "skills":[{"name":"","level":"EXPERT","mainSkill":true}]}
                """;

        mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpectAll(status().isBadRequest(), jsonPath("$.message").value("Validation failed"));
        verifyNoInteractions(profileService);
    }

    @Test
    void getMissingProfile_returnsNotFound() throws Exception {
        when(profileService.getProfile("user@example.com")).thenThrow(new ProfileNotFoundException());

        mockMvc.perform(get("/api/profiles"))
                .andExpectAll(status().isNotFound(), jsonPath("$.message").value("Profile not found"));
    }

    @Test
    void updateProfile_returnsUpdatedProfile() throws Exception {
        when(profileService.updateProfile(eq("user@example.com"), any())).thenReturn(response());

        mockMvc.perform(put("/api/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpectAll(status().isOk(), jsonPath("$.targetRole").value("Backend Developer"));
    }

    @Test
    void deleteProfile_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/profiles")).andExpect(status().isNoContent());
        verify(profileService).deleteProfile("user@example.com");
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
