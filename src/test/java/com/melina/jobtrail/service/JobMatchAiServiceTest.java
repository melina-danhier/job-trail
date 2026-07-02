package com.melina.jobtrail.service;

import com.melina.jobtrail.dto.JobMatchRequest;
import com.melina.jobtrail.dto.JobMatchResponse;
import com.melina.jobtrail.dto.profile.ProfileResponse;
import com.melina.jobtrail.exception.AiServiceException;
import com.melina.jobtrail.exception.AiResponseParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobMatchAiServiceTest {

    @InjectMocks
    private JobMatchAiService jobMatchAiService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private ProfileService profileService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JobMatchRateLimiter rateLimiter;

    @Test
    void analyze_withValidAiResponse_returnsJobMatchResponse() {
        String email = "user@example.com";
        JobMatchRequest request = new JobMatchRequest("Java backend position");
        ProfileResponse profile = createProfileResponse();
        JobMatchResponse expectedResponse = new JobMatchResponse(
                85,
                List.of("Java"),
                List.of("Docker"),
                "Apply",
                "Good match"
        );

        when(profileService.getProfile(email)).thenReturn(profile);
        when(objectMapper.writeValueAsString(profile)).thenReturn("{\"targetRole\":\"Backend Developer\"}");
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(org.mockito.ArgumentMatchers.eq(JobMatchResponse.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(expectedResponse);

        JobMatchResponse response = jobMatchAiService.analyze(email, request);

        assertEquals(expectedResponse, response);
    }

    @Test
    void analyze_withInvalidAiResponse_throwsAiResponseParseException() {
        String email = "user@example.com";
        JobMatchRequest request = new JobMatchRequest("Java backend position");
        ProfileResponse profile = createProfileResponse();
        when(profileService.getProfile(email)).thenReturn(profile);
        when(objectMapper.writeValueAsString(profile)).thenReturn("{}");
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(org.mockito.ArgumentMatchers.eq(JobMatchResponse.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(null);

        assertThrows(
                AiResponseParseException.class,
                () -> jobMatchAiService.analyze(email, request)
        );
    }

    @Test
    void analyze_withSemanticallyInvalidAiResponse_throwsAiResponseParseException() {
        String email = "user@example.com";
        JobMatchRequest request = new JobMatchRequest("Java backend position");
        ProfileResponse profile = createProfileResponse();
        String resultJson = "{\"score\":101}";
        JobMatchResponse invalidResponse = new JobMatchResponse(
                101, List.of(), List.of(), "Apply", "Invalid score"
        );

        when(profileService.getProfile(email)).thenReturn(profile);
        when(objectMapper.writeValueAsString(profile)).thenReturn("{}");
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(org.mockito.ArgumentMatchers.eq(JobMatchResponse.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(invalidResponse);

        assertThrows(AiResponseParseException.class, () -> jobMatchAiService.analyze(email, request));
    }

    @Test
    void analyze_whenAiProviderFails_throwsAiServiceException() {
        String email = "user@example.com";
        JobMatchRequest request = new JobMatchRequest("Java backend position");
        ProfileResponse profile = createProfileResponse();

        when(profileService.getProfile(email)).thenReturn(profile);
        when(objectMapper.writeValueAsString(profile)).thenReturn("{}");
        when(chatClient.prompt().system(anyString()).user(anyString()).call())
                .thenThrow(new RuntimeException("provider unavailable"));

        assertThrows(AiServiceException.class, () -> jobMatchAiService.analyze(email, request));
    }

    private ProfileResponse createProfileResponse() {
        return new ProfileResponse(
                1L, "Backend Developer", "Berlin", "Immediately", null,
                null, List.of(), List.of(), List.of(), java.util.Set.of(), java.util.Set.of()
        );
    }
}
