package com.melina.jobtrail.security;

import com.melina.jobtrail.util.JwtUtil;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void invalidToken_shouldUseAuthenticationEntryPointAndStopFilterChain() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        CustomAuthenticationEntryPoint authenticationEntryPoint = mock(CustomAuthenticationEntryPoint.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil, userDetailsService, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer invalid-token");

        when(jwtUtil.validateTokenAndExtractEmail("invalid-token"))
                .thenThrow(new MalformedJwtException("Invalid token"));

        filter.doFilter(request, response, filterChain);

        verify(authenticationEntryPoint).commence(
                any(MockHttpServletRequest.class),
                any(MockHttpServletResponse.class),
                any(AuthenticationException.class)
        );
        verify(filterChain, never()).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
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
        assertTrue(authentication.isAuthenticated());
        assertSame(userDetails, authentication.getPrincipal());
        assertEquals(userDetails.getAuthorities(), authentication.getAuthorities());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(authenticationEntryPoint);
    }
}
