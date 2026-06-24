package com.melina.jobtrail.controller;


import com.melina.jobtrail.dto.CompanyDto;
import com.melina.jobtrail.dto.RequestCompanyDto;
import com.melina.jobtrail.exception.CompanyNotFoundException;
import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.security.JwtAuthFilter;
import com.melina.jobtrail.service.CompanyService;
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


@WebMvcTest(CompanyController.class)
@AutoConfigureMockMvc(addFilters = false)
class CompanyControllerTest {

    @MockitoBean
    private CompanyService companyService;

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
    void createCompany_withValidDto_returnsCreatedCompany() throws Exception {
        RequestCompanyDto requestDto = new RequestCompanyDto("Company Name", null, null);
        CompanyDto dto = new CompanyDto(1L, "Company Name", null, null);

        when(companyService.createCompany(any(String.class), any(RequestCompanyDto.class))).thenReturn(dto);

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(1L),
                        jsonPath("$.name").value("Company Name")
                );

        verify(companyService).createCompany(any(String.class), any(RequestCompanyDto.class));
    }

    @Test
    void createCompany_withInvalidDto_returnsBadRequest() throws Exception {
        RequestCompanyDto requestDto = new RequestCompanyDto(null, null, null);

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(companyService);
    }

    @Test
    void getAllCompanies_returnsAllCompanies() throws Exception {
        List<CompanyDto> companies = List.of(
                new CompanyDto(2L, "First Company", null, null),
                new CompanyDto(3L, "Second Company", null, null)
        );
        when(companyService.getAllCompanies(anyString())).thenReturn(companies);

        mockMvc.perform(get("/api/companies"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$").isArray(),
                        jsonPath("$.length()").value(2),
                        jsonPath("$[0].name").value("First Company")
                );

        verify(companyService).getAllCompanies(anyString());
    }

    @Test
    void getCompanyById_withValidId_returnsCompany() throws Exception {
        CompanyDto company = new CompanyDto(4L, "Company Name", "https://example.com", "Berlin");
        when(companyService.getCompanyById(anyString(), eq(4L))).thenReturn(company);

        mockMvc.perform(get("/api/companies/{id}", 4L))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(4L),
                        jsonPath("$.name").value("Company Name"),
                        jsonPath("$.location").value("Berlin")
                );

        verify(companyService).getCompanyById(anyString(), eq(4L));
    }

    @Test
    void getCompanyById_withInvalidId_returnsNotFound() throws Exception {
        when(companyService.getCompanyById(anyString(), eq(5L)))
                .thenThrow(new CompanyNotFoundException(5L));

        mockMvc.perform(get("/api/companies/{id}", 5L))
                .andExpect(status().isNotFound());

        verify(companyService).getCompanyById(anyString(), eq(5L));
    }

    @Test
    void updateCompany_withValidDtoAndId_returnsUpdatedCompany() throws Exception {
        RequestCompanyDto requestDto = new RequestCompanyDto("Updated Company", null, "Hamburg");
        CompanyDto company = new CompanyDto(6L, "Updated Company", null, "Hamburg");
        when(companyService.updateCompany(anyString(), eq(6L), any(RequestCompanyDto.class)))
                .thenReturn(company);

        mockMvc.perform(put("/api/companies/{id}", 6L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(6L),
                        jsonPath("$.name").value("Updated Company"),
                        jsonPath("$.location").value("Hamburg")
                );

        verify(companyService).updateCompany(anyString(), eq(6L), any(RequestCompanyDto.class));
    }

    @Test
    void updateCompany_withInvalidId_returnsNotFound() throws Exception {
        RequestCompanyDto requestDto = new RequestCompanyDto("Updated Company", null, null);
        when(companyService.updateCompany(anyString(), eq(7L), any(RequestCompanyDto.class)))
                .thenThrow(new CompanyNotFoundException(7L));

        mockMvc.perform(put("/api/companies/{id}", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());

        verify(companyService).updateCompany(anyString(), eq(7L), any(RequestCompanyDto.class));
    }

    @Test
    void updateCompany_withInvalidDto_returnsBadRequest() throws Exception {
        RequestCompanyDto requestDto = new RequestCompanyDto(null, null, null);

        mockMvc.perform(put("/api/companies/{id}", 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(companyService);
    }

    @Test
    void deleteCompany_withValidId_returnsNoContent() throws Exception {
        doNothing().when(companyService).deleteCompany(anyString(), eq(9L));

        mockMvc.perform(delete("/api/companies/{id}", 9L))
                .andExpect(status().isNoContent());

        verify(companyService).deleteCompany(anyString(), eq(9L));
    }

    @Test
    void deleteCompany_withInvalidId_returnsNotFound() throws Exception {
        doThrow(new CompanyNotFoundException(10L))
                .when(companyService).deleteCompany(anyString(), eq(10L));

        mockMvc.perform(delete("/api/companies/{id}", 10L))
                .andExpect(status().isNotFound());

        verify(companyService).deleteCompany(anyString(), eq(10L));
    }
}
