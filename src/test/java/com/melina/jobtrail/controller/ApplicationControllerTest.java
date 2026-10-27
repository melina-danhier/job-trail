package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.CompanyResponse;
import com.melina.jobtrail.dto.PageResponse;
import com.melina.jobtrail.dto.application.ApplicationCreateRequest;
import com.melina.jobtrail.dto.application.ApplicationResponse;
import com.melina.jobtrail.dto.application.ApplicationStatusHistoryResponse;
import com.melina.jobtrail.dto.application.ApplicationUpdateRequest;
import com.melina.jobtrail.dto.application.ApplicationUpdateStatusRequest;
import com.melina.jobtrail.exception.ApplicationNotFoundException;
import com.melina.jobtrail.exception.DuplicateApplicationException;
import com.melina.jobtrail.service.ApplicationService;
import com.melina.jobtrail.util.ApplicationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApplicationControllerTest extends AbstractControllerTest {

    @MockitoBean private ApplicationService applicationService;

    @Test
    void createApplication_withValidDto_returnsCreatedApplication() throws Exception {
        ApplicationCreateRequest request = createRequest("Software Engineer");
        ApplicationResponse response = createApplicationResponse(1L, "Software Engineer");

        when(applicationService.createApplication(anyString(), eq(request)))
                .thenReturn(response);

        mvcWithRequest(
                post("/api/applications"),
                objectMapper.writeValueAsString(request)
        ).andExpectAll(
                status().isCreated(),
                jsonPath("$.id").value(response.id()),
                jsonPath("$.positionTitle").value("Software Engineer")
        );

        verify(applicationService).createApplication(eq("user@example.com"), eq(request));
    }

    @Test
    void createApplication_withInvalidDto_returnsBadRequest() throws Exception {
        ApplicationCreateRequest request = createRequest(null);

        mvcWithRequest(
                post("/api/applications"),
                objectMapper.writeValueAsString(request)
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(applicationService);
    }

    @Test
    void createApplication_withMissingCompanyId_returnsBadRequestAndFieldErrors() throws Exception {
        mvcWithRequest(
                post("/api/applications"),
                "{\"positionTitle\": \"Backend Developer\"}"
        ).andExpectAll(
                status().isBadRequest(),
                jsonPath("$.status").value(400),
                jsonPath("$.error").value("Bad Request"),
                jsonPath("$.message").value("Validation failed"),
                jsonPath("$.fieldErrors.companyId").value("Company ID cannot be null")
        );

        verifyNoInteractions(applicationService);
    }

    @Test
    void createApplication_withBlankPositionTitle_returnsBadRequestAndFieldErrors() throws Exception {
        mvcWithRequest(
                post("/api/applications"),
                "{\"positionTitle\": \"   \", \"companyId\": 1}"
        ).andExpectAll(
                status().isBadRequest(),
                jsonPath("$.status").value(400),
                jsonPath("$.message").value("Validation failed"),
                jsonPath("$.fieldErrors.positionTitle").value("Position title cannot be null or blank")
        );

        verifyNoInteractions(applicationService);
    }

    @Test
    void createApplication_withInvalidJobUrl_returnsBadRequestAndFieldErrors() throws Exception {
        mvcWithRequest(
                post("/api/applications"),
                "{\"positionTitle\": \"Java Developer\", \"companyId\": 1, \"jobUrl\": \"not-a-valid-url\"}"
        ).andExpectAll(
                status().isBadRequest(),
                jsonPath("$.status").value(400),
                jsonPath("$.message").value("Validation failed"),
                jsonPath("$.fieldErrors.jobUrl").value("Invalid job URL format")
        );

        verifyNoInteractions(applicationService);
    }

    @Test
    void createApplication_whenDuplicatePositionAndCompany_returnsConflict() throws Exception {
        ApplicationCreateRequest request = createRequest("Senior Developer");
        when(applicationService.createApplication(anyString(), eq(request)))
                .thenThrow(new DuplicateApplicationException());

        mvcWithRequest(
                post("/api/applications"),
                objectMapper.writeValueAsString(request)
        ).andExpectAll(
                status().isConflict(),
                jsonPath("$.status").value(409),
                jsonPath("$.error").value("Conflict"),
                jsonPath("$.message").value("An application for this company and position already exists")
        );
    }

    @Test
    void getApplications_withDefaultParameters_returnsFirstPage() throws Exception {
        List<ApplicationResponse> applications = List.of(
                createApplicationResponse(1L, "Backend Developer")
        );
        PageResponse<ApplicationResponse> response = new PageResponse<>(
                applications, 0, 20, 1, 1, true, true
        );

        when(applicationService.getApplications(
                anyString(), eq(0), eq(20), eq("createdAt"), eq(Sort.Direction.DESC),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(response);

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
                eq("user@example.com"), eq(0), eq(20), eq("createdAt"), eq(Sort.Direction.DESC),
                isNull(), isNull(), isNull(), isNull()
        );
    }

    @Test
    void getApplications_withPaginationAndSorting_returnsRequestedPage() throws Exception {
        PageResponse<ApplicationResponse> response = new PageResponse<>(
                List.of(), 2, 5, 10, 2, false, true
        );

        when(applicationService.getApplications(
                anyString(), eq(2), eq(5), eq("applicationDate"), eq(Sort.Direction.ASC),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(response);

        mockMvc.perform(get("/api/applications")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sortBy", "applicationDate")
                        .param("direction", "ASC"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.page").value(2),
                        jsonPath("$.size").value(5)
                );

        verify(applicationService).getApplications(
                eq("user@example.com"), eq(2), eq(5), eq("applicationDate"), eq(Sort.Direction.ASC),
                isNull(), isNull(), isNull(), isNull()
        );
    }

    @Test
    void getApplications_withCombinedFilters_returnsMatchingPage() throws Exception {
        PageResponse<ApplicationResponse> response = new PageResponse<>(
                List.of(), 0, 20, 0, 0, true, true
        );

        when(applicationService.getApplications(
                anyString(), eq(0), eq(20), eq("createdAt"), eq(Sort.Direction.DESC),
                eq(ApplicationStatus.INTERVIEW_SCHEDULED), eq(4L),
                eq(LocalDate.of(2026, 1, 1)),
                eq(LocalDate.of(2026, 6, 30))
        )).thenReturn(response);

        mockMvc.perform(get("/api/applications")
                        .param("status", "INTERVIEW_SCHEDULED")
                        .param("companyId", "4")
                        .param("applicationDateFrom", "2026-01-01")
                        .param("applicationDateTo", "2026-06-30"))
                .andExpect(status().isOk());

        verify(applicationService).getApplications(
                eq("user@example.com"), eq(0), eq(20), eq("createdAt"), eq(Sort.Direction.DESC),
                eq(ApplicationStatus.INTERVIEW_SCHEDULED), eq(4L),
                eq(LocalDate.of(2026, 1, 1)),
                eq(LocalDate.of(2026, 6, 30))
        );
    }

    @Test
    void getApplications_withOversizedPage_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/applications").param("size", "101"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.message").value("Invalid pagination parameters")
                );

        verifyNoInteractions(applicationService);
    }

    @Test
    void getApplications_withNegativePage_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/applications").param("page", "-1"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.message").value("Invalid pagination parameters")
                );

        verifyNoInteractions(applicationService);
    }

    @Test
    void getApplications_withZeroPageSize_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/applications").param("size", "0"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.message").value("Invalid pagination parameters")
                );

        verifyNoInteractions(applicationService);
    }

    @Test
    void getApplications_withInvalidDirection_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/applications").param("direction", "SIDEWAYS"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(applicationService);
    }

    @Test
    void getApplications_withInvalidSortField_returnsBadRequest() throws Exception {
        when(applicationService.getApplications(
                anyString(), anyInt(), anyInt(), eq("invalidField"), any(),
                any(), any(), any(), any()
        )).thenThrow(new IllegalArgumentException("Invalid sortBy value. Allowed values: createdAt, updatedAt, applicationDate, positionTitle, status"));

        mockMvc.perform(get("/api/applications").param("sortBy", "invalidField"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.error").value("Bad Request"),
                        jsonPath("$.message").value("Invalid sortBy value. Allowed values: createdAt, updatedAt, applicationDate, positionTitle, status")
                );
    }

    @Test
    void getApplications_withContradictoryDates_returnsBadRequest() throws Exception {
        when(applicationService.getApplications(
                anyString(), anyInt(), anyInt(), anyString(), any(),
                any(), any(), eq(LocalDate.of(2026, 12, 31)), eq(LocalDate.of(2026, 1, 1))
        )).thenThrow(new IllegalArgumentException("applicationDateFrom must not be after applicationDateTo"));

        mockMvc.perform(get("/api/applications")
                        .param("applicationDateFrom", "2026-12-31")
                        .param("applicationDateTo", "2026-01-01"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.error").value("Bad Request"),
                        jsonPath("$.message").value("applicationDateFrom must not be after applicationDateTo")
                );
    }

    @Test
    void getApplicationById_withValidId_returnsApplication() throws Exception {
        ApplicationResponse response = createApplicationResponse(3L, "Frontend Developer");

        when(applicationService.getApplicationById("user@example.com", 3L))
                .thenReturn(response);

        mockMvc.perform(get("/api/applications/{id}", 3L))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(response.id()),
                        jsonPath("$.positionTitle").value(response.positionTitle())
                );

        verify(applicationService).getApplicationById("user@example.com", 3L);
    }

    @Test
    void getApplicationById_whenApplicationDoesNotExist_returnsNotFound() throws Exception {
        when(applicationService.getApplicationById("user@example.com", 3L))
                .thenThrow(new ApplicationNotFoundException(3L));

        mockMvc.perform(get("/api/applications/{id}", 3L))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404),
                        jsonPath("$.error").value("Not Found"),
                        jsonPath("$.message").value("Application with id 3 not found")
                );

        verify(applicationService).getApplicationById("user@example.com", 3L);
    }

    @Test
    void updateApplication_withValidDtoAndId_returnsUpdatedApplication() throws Exception {
        ApplicationUpdateRequest request = updateRequest("Software Developer");
        ApplicationResponse response = createApplicationResponse(4L, "Software Developer");

        when(applicationService.updateApplication(
                anyString(), eq(4L), eq(request)
        )).thenReturn(response);

        mvcWithRequest(
                put("/api/applications/{id}", 4L),
                objectMapper.writeValueAsString(request)
        ).andExpectAll(
                status().isOk(),
                jsonPath("$.positionTitle").value("Software Developer")
        );

        verify(applicationService).updateApplication(
                eq("user@example.com"), eq(4L), eq(request)
        );
    }

    @Test
    void updateApplication_whenApplicationDoesNotExist_returnsNotFound() throws Exception {
        ApplicationUpdateRequest request = updateRequest("Software Developer");

        when(applicationService.updateApplication(
                anyString(), eq(5L), eq(request)
        )).thenThrow(new ApplicationNotFoundException(5L));

        mvcWithRequest(
                put("/api/applications/{id}", 5L),
                objectMapper.writeValueAsString(request)
        ).andExpectAll(
                status().isNotFound(),
                jsonPath("$.status").value(404),
                jsonPath("$.error").value("Not Found"),
                jsonPath("$.message").value("Application with id 5 not found")
        );

        verify(applicationService).updateApplication(
                eq("user@example.com"), eq(5L), eq(request)
        );
    }

    @Test
    void updateApplication_withInvalidDto_returnsBadRequest() throws Exception {
        ApplicationUpdateRequest request = updateRequest(null);

        mvcWithRequest(
                put("/api/applications/{id}", 6L),
                objectMapper.writeValueAsString(request)
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(applicationService);
    }

    @Test
    void updateApplication_withMissingCompanyId_returnsBadRequest() throws Exception {
        mvcWithRequest(
                put("/api/applications/{id}", 4L),
                "{\"positionTitle\": \"Backend Developer\"}"
        ).andExpectAll(
                status().isBadRequest(),
                jsonPath("$.status").value(400),
                jsonPath("$.fieldErrors.companyId").value("Company ID cannot be null")
        );

        verifyNoInteractions(applicationService);
    }

    @Test
    void updateApplication_withInvalidJobUrl_returnsBadRequest() throws Exception {
        mvcWithRequest(
                put("/api/applications/{id}", 4L),
                "{\"positionTitle\": \"Backend Developer\", \"companyId\": 1, \"jobUrl\": \"invalid-url\"}"
        ).andExpectAll(
                status().isBadRequest(),
                jsonPath("$.status").value(400),
                jsonPath("$.fieldErrors.jobUrl").value("Invalid job URL format")
        );

        verifyNoInteractions(applicationService);
    }

    @Test
    void updateApplication_whenDuplicate_returnsConflict() throws Exception {
        ApplicationUpdateRequest request = updateRequest("Senior Developer");
        when(applicationService.updateApplication(anyString(), eq(4L), eq(request)))
                .thenThrow(new DuplicateApplicationException());

        mvcWithRequest(
                put("/api/applications/{id}", 4L),
                objectMapper.writeValueAsString(request)
        ).andExpectAll(
                status().isConflict(),
                jsonPath("$.status").value(409),
                jsonPath("$.error").value("Conflict"),
                jsonPath("$.message").value("An application for this company and position already exists")
        );
    }

    @Test
    void updateApplicationStatus_withValidStatus_returnsUpdatedApplication() throws Exception {
        ApplicationUpdateStatusRequest request =
                new ApplicationUpdateStatusRequest(ApplicationStatus.INTERVIEW_SCHEDULED);
        ApplicationResponse response = createApplicationResponse(6L, "Software Developer");

        when(applicationService.updateApplicationStatus(
                "user@example.com", 6L, request
        )).thenReturn(response);

        mvcWithRequest(
                patch("/api/applications/{id}/status", 6L),
                objectMapper.writeValueAsString(request)
        ).andExpectAll(
                status().isOk(),
                jsonPath("$.id").value(6L),
                jsonPath("$.positionTitle").value("Software Developer")
        );

        verify(applicationService).updateApplicationStatus("user@example.com", 6L, request);
    }

    @Test
    void updateApplicationStatus_withMissingStatus_returnsBadRequest() throws Exception {
        mvcWithRequest(
                patch("/api/applications/{id}/status", 6L),
                "{}"
        ).andExpectAll(
                status().isBadRequest(),
                jsonPath("$.fieldErrors.status").value("Status cannot be null")
        );

        verifyNoInteractions(applicationService);
    }

    @Test
    void updateApplicationStatus_whenApplicationNotFound_returnsNotFound() throws Exception {
        ApplicationUpdateStatusRequest request = new ApplicationUpdateStatusRequest(ApplicationStatus.ACCEPTED);
        when(applicationService.updateApplicationStatus(anyString(), eq(99L), eq(request)))
                .thenThrow(new ApplicationNotFoundException(99L));

        mvcWithRequest(
                patch("/api/applications/{id}/status", 99L),
                objectMapper.writeValueAsString(request)
        ).andExpectAll(
                status().isNotFound(),
                jsonPath("$.status").value(404),
                jsonPath("$.error").value("Not Found"),
                jsonPath("$.message").value("Application with id 99 not found")
        );
    }

    @Test
    void getStatusHistory_returnsStatusTransitions() throws Exception {
        Instant changedAt = Instant.parse("2026-02-01T09:30:00Z");
        List<ApplicationStatusHistoryResponse> history = List.of(
                new ApplicationStatusHistoryResponse(
                        12L, ApplicationStatus.SAVED, ApplicationStatus.APPLIED, changedAt
                )
        );

        when(applicationService.getStatusHistory("user@example.com", 7L))
                .thenReturn(history);

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
    void getStatusHistory_whenApplicationNotFound_returnsNotFound() throws Exception {
        when(applicationService.getStatusHistory(anyString(), eq(99L)))
                .thenThrow(new ApplicationNotFoundException(99L));

        mockMvc.perform(get("/api/applications/{id}/status-history", 99L))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404),
                        jsonPath("$.error").value("Not Found"),
                        jsonPath("$.message").value("Application with id 99 not found")
                );
    }

    @Test
    void deleteApplication_withValidId_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/applications/{id}", 7L))
                .andExpect(status().isNoContent());

        verify(applicationService).deleteApplication("user@example.com", 7L);
    }

    @Test
    void deleteApplication_whenApplicationDoesNotExist_returnsNotFound() throws Exception {
        doThrow(new ApplicationNotFoundException(8L))
                .when(applicationService).deleteApplication("user@example.com", 8L);

        mockMvc.perform(delete("/api/applications/{id}", 8L))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404),
                        jsonPath("$.error").value("Not Found"),
                        jsonPath("$.message").value("Application with id 8 not found")
                );

        verify(applicationService).deleteApplication("user@example.com", 8L);
    }

    private ResultActions mvcWithRequest(
            MockHttpServletRequestBuilder requestBuilder,
            String requestString
    ) throws Exception {
        return mockMvc.perform(requestBuilder
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestString)
        );
    }

    private ApplicationResponse createApplicationResponse(long id, String positionTitle) {
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

    private ApplicationCreateRequest createRequest(String positionTitle) {
        return new ApplicationCreateRequest(positionTitle, 1L, null, null);
    }

    private ApplicationUpdateRequest updateRequest(String positionTitle) {
        return new ApplicationUpdateRequest(positionTitle, 1L, null, null);
    }
}
