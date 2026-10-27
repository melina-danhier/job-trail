package com.melina.jobtrail.security;

import com.melina.jobtrail.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Valid JWT token authenticates user with authorities and continues filter chain")
    void validToken_shouldAuthenticateUserAndContinueFilterChain() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        CustomAuthenticationEntryPoint authenticationEntryPoint = mock(CustomAuthenticationEntryPoint.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil, userDetailsService, authenticationEntryPoint);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CustomUserDetails userDetails = new CustomUserDetails("user@example.com", "password-hash");
        request.addHeader("Authorization", "Bearer valid-token");

        when(jwtUtil.validateTokenAndExtractEmail("valid-token")).thenReturn(userDetails.email());
        when(userDetailsService.loadUserByUsername(userDetails.email())).thenReturn(userDetails);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertSame(userDetails, authentication.getPrincipal());
        assertEquals(userDetails.getAuthorities(), authentication.getAuthorities());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(authenticationEntryPoint);
    }

    @Test
    @DisplayName("Missing Authorization header continues filter chain without authenticating")
    void missingAuthHeader_shouldContinueFilterChainWithoutAuthenticating() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        CustomAuthenticationEntryPoint authenticationEntryPoint = mock(CustomAuthenticationEntryPoint.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil, userDetailsService, authenticationEntryPoint);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil, userDetailsService, authenticationEntryPoint);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic dXNlcjpwYXNz", "Digest abcdef", "Token 12345", "Bearer"})
    @DisplayName("Non-Bearer or incomplete authorization headers continue filter chain without authenticating")
    void nonBearerHeader_shouldContinueFilterChainWithoutAuthenticating(String authHeader) throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        CustomAuthenticationEntryPoint authenticationEntryPoint = mock(CustomAuthenticationEntryPoint.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil, userDetailsService, authenticationEntryPoint);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", authHeader);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil, userDetailsService, authenticationEntryPoint);
    }

    @Test
    @DisplayName("Expired token triggers authentication entry point and stops filter chain")
    void expiredToken_shouldUseAuthenticationEntryPointAndStopFilterChain() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        CustomAuthenticationEntryPoint authenticationEntryPoint = mock(CustomAuthenticationEntryPoint.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil, userDetailsService, authenticationEntryPoint);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer expired-token");

        when(jwtUtil.validateTokenAndExtractEmail("expired-token"))
                .thenThrow(new ExpiredJwtException(null, null, "Token has expired"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationEntryPoint).commence(
                any(MockHttpServletRequest.class),
                any(MockHttpServletResponse.class),
                any(AuthenticationException.class)
        );
        verify(filterChain, never()).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    @DisplayName("Malformed token triggers authentication entry point and stops filter chain")
    void malformedToken_shouldUseAuthenticationEntryPointAndStopFilterChain() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        CustomAuthenticationEntryPoint authenticationEntryPoint = mock(CustomAuthenticationEntryPoint.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil, userDetailsService, authenticationEntryPoint);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer malformed-token");

        when(jwtUtil.validateTokenAndExtractEmail("malformed-token"))
                .thenThrow(new MalformedJwtException("Malformed JWT"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationEntryPoint).commence(
                any(MockHttpServletRequest.class),
                any(MockHttpServletResponse.class),
                any(AuthenticationException.class)
        );
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Untrusted signature triggers authentication entry point and stops filter chain")
    void untrustedSignature_shouldUseAuthenticationEntryPointAndStopFilterChain() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        CustomAuthenticationEntryPoint authenticationEntryPoint = mock(CustomAuthenticationEntryPoint.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil, userDetailsService, authenticationEntryPoint);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer untrusted-signature-token");

        when(jwtUtil.validateTokenAndExtractEmail("untrusted-signature-token"))
                .thenThrow(new SecurityException("Signature invalid"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationEntryPoint).commence(
                any(MockHttpServletRequest.class),
                any(MockHttpServletResponse.class),
                any(AuthenticationException.class)
        );
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Token for non-existing user triggers authentication entry point and clears context")
    void unknownUser_shouldUseAuthenticationEntryPointAndStopFilterChain() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        CustomAuthenticationEntryPoint authenticationEntryPoint = mock(CustomAuthenticationEntryPoint.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil, userDetailsService, authenticationEntryPoint);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer valid-token-unknown-user");

        when(jwtUtil.validateTokenAndExtractEmail("valid-token-unknown-user"))
                .thenReturn("ghost@example.com");
        when(userDetailsService.loadUserByUsername("ghost@example.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationEntryPoint).commence(
                any(MockHttpServletRequest.class),
                any(MockHttpServletResponse.class),
                any(AuthenticationException.class)
        );
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Empty bearer token string triggers authentication entry point and stops filter chain")
    void emptyBearerToken_shouldUseAuthenticationEntryPointAndStopFilterChain() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        CustomAuthenticationEntryPoint authenticationEntryPoint = mock(CustomAuthenticationEntryPoint.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil, userDetailsService, authenticationEntryPoint);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer ");

        when(jwtUtil.validateTokenAndExtractEmail(""))
                .thenThrow(new IllegalArgumentException("Token cannot be blank"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationEntryPoint).commence(
                any(MockHttpServletRequest.class),
                any(MockHttpServletResponse.class),
                any(AuthenticationException.class)
        );
        verify(filterChain, never()).doFilter(request, response);
    }
}
