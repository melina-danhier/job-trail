package com.melina.jobtrail.controller;

import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.security.JwtAuthFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc(addFilters = false)
public abstract class AbstractControllerTest {

    @MockitoBean protected JwtAuthFilter jwtAuthFilter;
    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    @BeforeEach
    void authenticate() {
        CustomUserDetails details = new CustomUserDetails("user@example.com", "passwordHash");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities())
        );
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }
}
