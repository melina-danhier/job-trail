package com.melina.jobtrail.controller;


import com.melina.jobtrail.dto.ApplicationDto;
import com.melina.jobtrail.dto.CompanyDto;
import com.melina.jobtrail.dto.RequestApplicationDto;
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
        RequestApplicationDto requestDto = createRequestApplicationDto("Software Engineer");
        ApplicationDto dto = createApplicationDto(1L, "Software Engineer");
        when(applicationService.createApplication(any(String.class), any(RequestApplicationDto.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(dto.id()),
                        jsonPath("$.positionTitle").value("Software Engineer")
                );

        verify(applicationService).createApplication(any(String.class), any(RequestApplicationDto.class));
    }

    @Test
    void createApplication_withInvalidDto_returnsBadRequest() throws Exception {
        RequestApplicationDto requestDto = createRequestApplicationDto(null);
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(applicationService);
    }

    @Test
    void getApplications_returnsAllApplications() throws Exception {
        List<ApplicationDto> applications = List.of(
                createApplicationDto(2L, "Backend Developer")
        );
        when(applicationService.getApplications(any(String.class)))
                .thenReturn(applications);

        mockMvc.perform(get("/api/applications"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$").isNotEmpty()
                );

        verify(applicationService).getApplications(any(String.class));
    }

    @Test
    void getApplicationById_withValidId_returnsApplication() throws Exception {
        ApplicationDto applicationDto = createApplicationDto(3L,"Frontend Developer");
        when(applicationService.getApplicationById(any(String.class), eq(3L)))
                .thenReturn(applicationDto);

        mockMvc.perform(get("/api/applications/{id}", 3L))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(applicationDto.id()),
                        jsonPath("$.positionTitle").value(applicationDto.positionTitle())
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
        ApplicationDto applicationDto = createApplicationDto(4L, "Software Developer");
        RequestApplicationDto requestDto = createRequestApplicationDto("Software Developer");
        when(applicationService.updateApplication(any(String.class), anyLong(), any(RequestApplicationDto.class)))
                .thenReturn(applicationDto);

        mockMvc.perform(put("/api/applications/{id}", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.positionTitle").value("Software Developer")
                );

        verify(applicationService).updateApplication(any(String.class), anyLong(), any(RequestApplicationDto.class));
    }

    @Test
    void updateApplication_withInvalidId_returnsNotFound() throws Exception {
        RequestApplicationDto requestDto = createRequestApplicationDto("Software Developer");
        when(applicationService.updateApplication(any(String.class), anyLong(), any(RequestApplicationDto.class)))
                .thenThrow(new ApplicationNotFoundException(5L));

        mockMvc.perform(put("/api/applications/{id}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());

        verify(applicationService).updateApplication(any(String.class), anyLong(), any(RequestApplicationDto.class));
    }

    @Test
    void updateApplication_withInvalidDto_returnsBadRequest() throws Exception {
        RequestApplicationDto requestDto = createRequestApplicationDto(null);
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

    private ApplicationDto createApplicationDto(long id, String positionTitle) {
        return new ApplicationDto(
                id,
                new CompanyDto(1L, "Company Name", null, null),
                positionTitle,
                null,
                null,
                null,
                null,
                null
        );

    }

    private RequestApplicationDto createRequestApplicationDto(String positionTitle) {
        return new RequestApplicationDto(
                positionTitle,
                1L,
                null,
                null,
                null
        );
    }
}
