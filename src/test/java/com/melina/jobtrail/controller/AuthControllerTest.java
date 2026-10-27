package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.DemoLoginResponse;
import com.melina.jobtrail.dto.LoginRequest;
import com.melina.jobtrail.dto.RegisterRequest;
import com.melina.jobtrail.dto.UserResponse;
import com.melina.jobtrail.exception.EmailAlreadyExistsException;
import com.melina.jobtrail.exception.UserNotFoundException;
import com.melina.jobtrail.service.DemoDataService;
import com.melina.jobtrail.service.UserService;
import com.melina.jobtrail.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
class AuthControllerTest extends AbstractControllerTest {

    @MockitoBean private UserService userService;
    @MockitoBean private DemoDataService demoDataService;
    @MockitoBean private AuthenticationManager authenticationManager;
    @MockitoBean private JwtUtil jwtUtil;

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
        verify(demoDataService).initDemoData(true);
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
                        jsonPath("$.status").value(400),
                        jsonPath("$.error").value("Bad Request"),
                        jsonPath("$.message").value("Validation failed"),
                        jsonPath("$.fieldErrors.password").isNotEmpty()
                );

        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "invalidEmail"})
    void register_withInvalidEmail_shouldReturnBadRequest(String email) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(email, "password1234");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.error").value("Bad Request"),
                        jsonPath("$.message").value("Validation failed"),
                        jsonPath("$.fieldErrors.email").isNotEmpty()
                );

        verifyNoInteractions(userService);
    }

    @Test
    void register_withExistingEmail_shouldReturnConflict() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("user@example.com", "password1234");

        when(userService.createUser(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException(registerRequest.email()));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath("$.status").value(409),
                        jsonPath("$.error").value("Conflict"),
                        jsonPath("$.message").value("User with Email user@example.com already exists")
                );

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
    void login_withInvalidCredentials_returnsUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest("user@example.com", "invalid-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath("$.status").value(401),
                        jsonPath("$.error").value("Unauthorized"),
                        jsonPath("$.message").value("Invalid credentials")
                );

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void login_withBlankEmail_returnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest("   ", "password1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.error").value("Bad Request"),
                        jsonPath("$.message").value("Validation failed"),
                        jsonPath("$.fieldErrors.email").isNotEmpty()
                );

        verifyNoInteractions(authenticationManager);
    }

    @Test
    void login_withBlankPassword_returnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest("user@example.com", "   ");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.error").value("Bad Request"),
                        jsonPath("$.message").value("Validation failed"),
                        jsonPath("$.fieldErrors.password").isNotEmpty()
                );

        verifyNoInteractions(authenticationManager);
    }

    @Test
    void getMe_returnsAuthenticatedUser() throws Exception {
        UserResponse userResponse = new UserResponse(
                "user@example.com",
                Instant.parse("2026-06-24T12:00:00Z")
        );

        when(userService.getMe("user@example.com")).thenReturn(userResponse);

        mockMvc.perform(get("/api/auth/me"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.email").value("user@example.com"),
                        jsonPath("$.createdAt").value("2026-06-24T12:00:00Z")
                );

        verify(userService).getMe("user@example.com");
    }

    @Test
    void getMe_whenUserNotFound_returnsNotFound() throws Exception {
        when(userService.getMe("user@example.com"))
                .thenThrow(new UserNotFoundException("user@example.com"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404),
                        jsonPath("$.error").value("Not Found"),
                        jsonPath("$.message").value("User with email user@example.com not found")
                );

        verify(userService).getMe("user@example.com");
    }

    @Test
    void login_withDemoEmail_shouldResetDemoData() throws Exception {
        LoginRequest loginRequest = new LoginRequest(DemoDataService.DEMO_EMAIL, "JobTrail2026!");

        when(jwtUtil.generateToken(loginRequest.email())).thenReturn("demo-jwt");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpectAll(
                        status().isOk(),
                        content().string("demo-jwt")
                );

        verify(demoDataService).initDemoData(true);
        verify(jwtUtil).generateToken(loginRequest.email());
    }

    @Test
    void demoLogin_defaultResetTrue_shouldReturnDemoResponse() throws Exception {
        DemoLoginResponse demoResponse = new DemoLoginResponse(
                "mocked-demo-token",
                "Bearer",
                "demo@jobtrail.dev",
                "Demo-Login erfolgreich!"
        );
        when(demoDataService.getOrCreateDemoToken(true)).thenReturn(demoResponse);

        mockMvc.perform(post("/api/auth/demo"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.token").value("mocked-demo-token"),
                        jsonPath("$.tokenType").value("Bearer"),
                        jsonPath("$.email").value("demo@jobtrail.dev"),
                        jsonPath("$.message").value("Demo-Login erfolgreich!")
                );

        verify(demoDataService).getOrCreateDemoToken(true);
    }

    @Test
    void demoLogin_withResetFalse_shouldPassFlagToService() throws Exception {
        DemoLoginResponse demoResponse = new DemoLoginResponse(
                "reused-demo-token",
                "Bearer",
                "demo@jobtrail.dev",
                "Demo-Login erfolgreich!"
        );
        when(demoDataService.getOrCreateDemoToken(false)).thenReturn(demoResponse);

        mockMvc.perform(post("/api/auth/demo").param("reset", "false"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.token").value("reused-demo-token")
                );

        verify(demoDataService).getOrCreateDemoToken(false);
    }
}
