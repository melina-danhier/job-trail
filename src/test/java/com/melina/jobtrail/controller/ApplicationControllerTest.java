package com.melina.jobtrail.controller;


import com.melina.jobtrail.dto.application.ApplicationRequest;
import com.melina.jobtrail.dto.application.ApplicationResponse;
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
        ApplicationRequest requestDto = createRequestApplicationDto("Software Engineer");
        ApplicationResponse dto = createApplicationDto(1L, "Software Engineer");
        when(applicationService.createApplication(any(String.class), any(ApplicationRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(dto.id()),
                        jsonPath("$.positionTitle").value("Software Engineer")
                );

        verify(applicationService).createApplication(any(String.class), any(ApplicationRequest.class));
    }

    @Test
    void createApplication_withInvalidDto_returnsBadRequest() throws Exception {
        ApplicationRequest requestDto = createRequestApplicationDto(null);
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
    void getApplications_withPaginationAndSorting_passesParametersToService() throws Exception {
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
    void getApplications_withFilters_passesParametersToService() throws Exception {
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
        ApplicationRequest requestDto = createRequestApplicationDto("Software Developer");
        when(applicationService.updateApplication(any(String.class), anyLong(), any(ApplicationRequest.class)))
                .thenReturn(applicationResponse);

        mockMvc.perform(put("/api/applications/{id}", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.positionTitle").value("Software Developer")
                );

        verify(applicationService).updateApplication(any(String.class), anyLong(), any(ApplicationRequest.class));
    }

    @Test
    void updateApplication_withInvalidId_returnsNotFound() throws Exception {
        ApplicationRequest requestDto = createRequestApplicationDto("Software Developer");
        when(applicationService.updateApplication(any(String.class), anyLong(), any(ApplicationRequest.class)))
                .thenThrow(new ApplicationNotFoundException(5L));

        mockMvc.perform(put("/api/applications/{id}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());

        verify(applicationService).updateApplication(any(String.class), anyLong(), any(ApplicationRequest.class));
    }

    @Test
    void updateApplication_withInvalidDto_returnsBadRequest() throws Exception {
        ApplicationRequest requestDto = createRequestApplicationDto(null);
        mockMvc.perform(put("/api/applications/{id}", 6L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(applicationService);
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

    private ApplicationRequest createRequestApplicationDto(String positionTitle) {
        return new ApplicationRequest(
                positionTitle,
                1L,
                null,
                null,
                null
        );
    }
}
