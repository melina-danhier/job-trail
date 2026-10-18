package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.JobMatchRequest;
import com.melina.jobtrail.dto.JobMatchResponse;
import com.melina.jobtrail.exception.AiServiceException;
import com.melina.jobtrail.exception.AiRateLimitException;
import com.melina.jobtrail.exception.AiFeatureDisabledException;
import com.melina.jobtrail.service.JobMatchAiService;
import com.melina.jobtrail.util.JobMatchRecommendation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobMatchController.class)
@AutoConfigureMockMvc(addFilters = false)
class JobMatchControllerTest extends AbstractControllerTest {

    @MockitoBean private JobMatchAiService jobMatchAiService;


    @Test
    void analyze_returnsMatch() throws Exception {
        JobMatchRequest request = new JobMatchRequest("Java backend role");
        when(jobMatchAiService.analyze(eq("user@example.com"), any())).thenReturn(
                new JobMatchResponse(85, List.of("Java"), List.of("Docker"), JobMatchRecommendation.APPLY, "Good fit"));

        mockMvc.perform(post("/api/job-match/analyze").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(status().isOk(), jsonPath("$.score").value(85),
                        jsonPath("$.recommendation").value("APPLY"));
    }

    @Test
    void analyze_withBlankDescription_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/job-match/analyze").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(jobMatchAiService);
    }

    @Test
    void analyze_whenProviderFails_returnsBadGateway() throws Exception {
        when(jobMatchAiService.analyze(eq("user@example.com"), any()))
                .thenThrow(new AiServiceException(new RuntimeException()));

        mockMvc.perform(post("/api/job-match/analyze").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Java backend role\"}"))
                .andExpectAll(status().isBadGateway(),
                        jsonPath("$.message").value("AI service is temporarily unavailable"));
    }

    @Test
    void analyze_whenRateLimitIsExceeded_returnsTooManyRequests() throws Exception {
        when(jobMatchAiService.analyze(eq("user@example.com"), any()))
                .thenThrow(new AiRateLimitException());

        mockMvc.perform(post("/api/job-match/analyze").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Java backend role\"}"))
                .andExpectAll(status().isTooManyRequests(),
                        jsonPath("$.message").value("AI job matching rate limit exceeded"));
    }

    @Test
    void analyze_withoutConfiguredProvider_returnsServiceUnavailable() throws Exception {
        when(jobMatchAiService.analyze(eq("user@example.com"), any()))
                .thenThrow(new AiFeatureDisabledException());

        mockMvc.perform(post("/api/job-match/analyze").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Java backend role\"}"))
                .andExpectAll(status().isServiceUnavailable(),
                        jsonPath("$.message").value("AI job matching is not configured"));
    }
}
