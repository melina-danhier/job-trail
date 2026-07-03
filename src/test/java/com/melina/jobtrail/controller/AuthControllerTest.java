package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.LoginRequest;
import com.melina.jobtrail.dto.RegisterRequest;
import com.melina.jobtrail.dto.UserResponse;
import com.melina.jobtrail.exception.EmailAlreadyExistsException;
import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.security.JwtAuthFilter;
import com.melina.jobtrail.service.UserService;
import com.melina.jobtrail.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtUtil jwtUtil;

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
    void register_withValidRequest_shouldReturnUserDto() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("user@example.com", "password1234");
        UserResponse userResponse = new UserResponse("user@example.com", null);

        when(userService.createUser(any(RegisterRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.email").value("user@example.com")
                );

        verify(userService).createUser(any(RegisterRequest.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "1234"})
    void register_withInvalidPassword_shouldReturnBadRequest(String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("user@example.com", password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.message").value("Validation failed"),
                        jsonPath("$.fieldErrors").isNotEmpty()
                );

        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "invalidEmail"})
    void register_withInvalidEmail_shouldReturnBadRequest(String email) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(email, "password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.message").value("Validation failed"),
                        jsonPath("$.fieldErrors").isNotEmpty()
                );

        verifyNoInteractions(userService);
    }

    @Test
    void register_withExistingEmail_shouldReturnConflict() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("user@example.com", "password");

        when(userService.createUser(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException(registerRequest.email()));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());

        verify(userService).createUser(any(RegisterRequest.class));
    }

    @Test
    void login_withValidCredentials_shouldReturnJwt() throws Exception {
        LoginRequest loginRequest = new LoginRequest("user@example.com", "password1234");

        when(jwtUtil.generateToken(loginRequest.email())).thenReturn("signed-jwt");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpectAll(
                        status().isOk(),
                        content().string("signed-jwt")
                );

        verify(authenticationManager).authenticate(argThat(authentication ->
                authentication instanceof UsernamePasswordAuthenticationToken
                        && loginRequest.email().equals(authentication.getPrincipal())
                        && loginRequest.password().equals(authentication.getCredentials())
        ));
        verify(jwtUtil).generateToken(loginRequest.email());
    }

    @Test
    void login_withInvalidCredentials_returnsUnauthorized () throws Exception {
        LoginRequest loginRequest = new LoginRequest("user@example.com", "invalid-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void getMe_returnsAuthenticatedUser() throws Exception {
        UserResponse userResponse = new UserResponse("user@example.com", Instant.parse("2026-06-24T12:00:00Z"));
        when(userService.getMe("user@example.com")).thenReturn(userResponse);

        mockMvc.perform(get("/api/auth/me"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.email").value("user@example.com"),
                        jsonPath("$.createdAt").value("2026-06-24T12:00:00Z")
                );

        verify(userService).getMe("user@example.com");
    }
}
