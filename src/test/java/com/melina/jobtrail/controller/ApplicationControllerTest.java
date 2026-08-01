package com.melina.jobtrail.controller;


import com.melina.jobtrail.dto.application.ApplicationCreateRequest;
import com.melina.jobtrail.dto.application.ApplicationResponse;
import com.melina.jobtrail.dto.application.ApplicationStatusHistoryResponse;
import com.melina.jobtrail.dto.application.ApplicationUpdateStatusRequest;
import com.melina.jobtrail.dto.application.ApplicationUpdateRequest;
import com.melina.jobtrail.dto.CompanyResponse;
import com.melina.jobtrail.dto.PageResponse;
import com.melina.jobtrail.exception.ApplicationNotFoundException;
import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.security.JwtAuthFilter;
import com.melina.jobtrail.service.ApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.time.Instant;

import com.melina.jobtrail.util.ApplicationStatus;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApplicationControllerTest {

    @MockitoBean
    private ApplicationService applicationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUpAuthentication() {
        CustomUserDetails userDetails = new CustomUserDetails("user@example.com", "passwordHash");
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createApplication_withValidDto_returnsCreatedApplication() throws Exception {
        ApplicationCreateRequest requestDto = createApplicationCreateDto("Software Engineer");
        ApplicationResponse dto = createApplicationDto(1L, "Software Engineer");
        when(applicationService.createApplication(any(String.class), any(ApplicationCreateRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(dto.id()),
                        jsonPath("$.positionTitle").value("Software Engineer")
                );

        verify(applicationService).createApplication(any(String.class), any(ApplicationCreateRequest.class));
    }

    @Test
    void createApplication_withInvalidDto_returnsBadRequest() throws Exception {
        ApplicationCreateRequest requestDto = createApplicationCreateDto(null);
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(applicationService);
    }

    @Test
    void getApplications_returnsAllApplications() throws Exception {
        List<ApplicationResponse> applications = List.of(
                createApplicationDto(2L, "Backend Developer")
        );
        PageResponse<ApplicationResponse> response = new PageResponse<>(applications, 0, 20, 1, 1, true, true);
        when(applicationService.getApplications(
                anyString(), eq(0), eq(20), eq("createdAt"), eq(Sort.Direction.DESC),
                isNull(), isNull(), isNull(), isNull()
        ))
                .thenReturn(response);

        mockMvc.perform(get("/api/applications"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.content[0].positionTitle").value("Backend Developer"),
                        jsonPath("$.page").value(0),
                        jsonPath("$.size").value(20),
                        jsonPath("$.totalElements").value(1),
                        jsonPath("$.last").value(true)
                );

        verify(applicationService).getApplications(
                anyString(), eq(0), eq(20), eq("createdAt"), eq(Sort.Direction.DESC),
                isNull(), isNull(), isNull(), isNull()
        );
    }

    @Test
    void getApplications_withPaginationAndSorting_returnsRequestedPage() throws Exception {
        PageResponse<ApplicationResponse> response = new PageResponse<>(List.of(), 2, 5, 10, 2, false, true);
        when(applicationService.getApplications(
                anyString(), eq(2), eq(5), eq("applicationDate"), eq(Sort.Direction.ASC),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(response);

        mockMvc.perform(get("/api/applications")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sortBy", "applicationDate")
                        .param("direction", "ASC"))
                .andExpectAll(status().isOk(), jsonPath("$.page").value(2));

        verify(applicationService).getApplications(
                anyString(), eq(2), eq(5), eq("applicationDate"), eq(Sort.Direction.ASC),
                isNull(), isNull(), isNull(), isNull()
        );
    }

    @Test
    void getApplications_withCombinedFilters_returnsMatchingPage() throws Exception {
        PageResponse<ApplicationResponse> response = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        when(applicationService.getApplications(
                anyString(), eq(0), eq(20), eq("createdAt"), eq(Sort.Direction.DESC),
                eq(com.melina.jobtrail.util.ApplicationStatus.INTERVIEW_SCHEDULED), eq(4L),
                eq(java.time.LocalDate.of(2026, 1, 1)), eq(java.time.LocalDate.of(2026, 6, 30))
        )).thenReturn(response);

        mockMvc.perform(get("/api/applications")
                        .param("status", "INTERVIEW_SCHEDULED")
                        .param("companyId", "4")
                        .param("applicationDateFrom", "2026-01-01")
                        .param("applicationDateTo", "2026-06-30"))
                .andExpect(status().isOk());

        verify(applicationService).getApplications(
                anyString(), eq(0), eq(20), eq("createdAt"), eq(Sort.Direction.DESC),
                eq(com.melina.jobtrail.util.ApplicationStatus.INTERVIEW_SCHEDULED), eq(4L),
                eq(java.time.LocalDate.of(2026, 1, 1)), eq(java.time.LocalDate.of(2026, 6, 30))
        );
    }

    @Test
    void getApplications_withOversizedPage_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/applications").param("size", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(applicationService);
    }

    @Test
    void getApplications_withInvalidDirection_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/applications").param("direction", "SIDEWAYS"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(applicationService);
    }

    @Test
    void getApplicationById_withValidId_returnsApplication() throws Exception {
        ApplicationResponse applicationResponse = createApplicationDto(3L,"Frontend Developer");
        when(applicationService.getApplicationById(any(String.class), eq(3L)))
                .thenReturn(applicationResponse);

        mockMvc.perform(get("/api/applications/{id}", 3L))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(applicationResponse.id()),
                        jsonPath("$.positionTitle").value(applicationResponse.positionTitle())
                );

        verify(applicationService).getApplicationById(any(String.class), anyLong());
    }

    @Test
    void getApplicationById_withInvalidId_returnsNotFound() throws Exception {
        when(applicationService.getApplicationById(any(String.class), anyLong()))
                .thenThrow(new ApplicationNotFoundException(3L));

        mockMvc.perform(get("/api/applications/{id}", 3L))
                .andExpect(status().isNotFound());

        verify(applicationService).getApplicationById(any(String.class), anyLong());
    }

    @Test
    void updateApplication_withValidDtoAndId_returnsUpdatedApplication() throws Exception {
        ApplicationResponse applicationResponse = createApplicationDto(4L, "Software Developer");
        ApplicationUpdateRequest requestDto = createApplicationUpdateDto("Software Developer");
        when(applicationService.updateApplication(any(String.class), anyLong(), any(ApplicationUpdateRequest.class)))
                .thenReturn(applicationResponse);

        mockMvc.perform(put("/api/applications/{id}", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.positionTitle").value("Software Developer")
                );

        verify(applicationService).updateApplication(any(String.class), anyLong(), any(ApplicationUpdateRequest.class));
    }

    @Test
    void updateApplication_withInvalidId_returnsNotFound() throws Exception {
        ApplicationUpdateRequest requestDto = createApplicationUpdateDto("Software Developer");
        when(applicationService.updateApplication(any(String.class), anyLong(), any(ApplicationUpdateRequest.class)))
                .thenThrow(new ApplicationNotFoundException(5L));

        mockMvc.perform(put("/api/applications/{id}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());

        verify(applicationService).updateApplication(any(String.class), anyLong(), any(ApplicationUpdateRequest.class));
    }

    @Test
    void updateApplication_withInvalidDto_returnsBadRequest() throws Exception {
        ApplicationUpdateRequest requestDto = createApplicationUpdateDto(null);
        mockMvc.perform(put("/api/applications/{id}", 6L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(applicationService);
    }

    @Test
    void updateApplicationStatus_withValidStatus_returnsUpdatedApplication() throws Exception {
        ApplicationUpdateStatusRequest request =
                new ApplicationUpdateStatusRequest(ApplicationStatus.INTERVIEW_SCHEDULED);
        ApplicationResponse response = createApplicationDto(6L, "Software Developer");
        when(applicationService.updateApplicationStatus(anyString(), eq(6L), eq(request))).thenReturn(response);

        mockMvc.perform(patch("/api/applications/{id}/status", 6L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(6L),
                        jsonPath("$.positionTitle").value("Software Developer")
                );

        verify(applicationService).updateApplicationStatus("user@example.com", 6L, request);
    }

    @Test
    void updateApplicationStatus_withMissingStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/applications/{id}/status", 6L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.fieldErrors.status").value("Status cannot be null")
                );

        verifyNoInteractions(applicationService);
    }

    @Test
    void getStatusHistory_returnsStatusTransitions() throws Exception {
        Instant changedAt = Instant.parse("2026-02-01T09:30:00Z");
        List<ApplicationStatusHistoryResponse> history = List.of(
                new ApplicationStatusHistoryResponse(
                        12L, ApplicationStatus.SAVED, ApplicationStatus.APPLIED, changedAt
                )
        );
        when(applicationService.getStatusHistory("user@example.com", 7L)).thenReturn(history);

        mockMvc.perform(get("/api/applications/{id}/status-history", 7L))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$[0].id").value(12L),
                        jsonPath("$[0].previousStatus").value("SAVED"),
                        jsonPath("$[0].newStatus").value("APPLIED"),
                        jsonPath("$[0].changedAt").value("2026-02-01T09:30:00Z")
                );

        verify(applicationService).getStatusHistory("user@example.com", 7L);
    }

    @Test
    void deleteApplication_withValidId_returnsNoContent() throws Exception {
        doNothing().when(applicationService).deleteApplication(any(String.class), eq(7L));

        mockMvc.perform(delete("/api/applications/{id}", 7L))
                .andExpect(status().isNoContent());

        verify(applicationService).deleteApplication(any(String.class), eq(7L));
    }

    @Test
    void deleteApplication_withInvalidId_returnsNotFound() throws Exception {
        doThrow(ApplicationNotFoundException.class)
                .when(applicationService).deleteApplication(any(String.class), eq(8L));

        mockMvc.perform(delete("/api/applications/{id}", 8L))
                .andExpect(status().isNotFound());

        verify(applicationService).deleteApplication(any(String.class), eq(8L));
    }

    private ApplicationResponse createApplicationDto(long id, String positionTitle) {
        return new ApplicationResponse(
                id,
                new CompanyResponse(1L, "Company Name", null, null),
                positionTitle,
                null,
                null,
                null,
                null,
                null
        );

    }

    private ApplicationCreateRequest createApplicationCreateDto(String positionTitle) {
        return new ApplicationCreateRequest(positionTitle, 1L, null, null);
    }

    private ApplicationUpdateRequest createApplicationUpdateDto(String positionTitle) {
        return new ApplicationUpdateRequest(positionTitle, 1L, null, null);
    }
}
