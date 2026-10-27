package com.melina.jobtrail.security;

import com.melina.jobtrail.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7).trim();
                    if (token.isEmpty()) {
                        throw new BadCredentialsException("Bearer token is empty");
                    }
                    String email = jwtUtil.validateTokenAndExtractEmail(token);
                    if (email == null || email.isBlank()) {
                        throw new BadCredentialsException("Token has no valid subject");
                    }
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    if (userDetails == null) {
                        throw new UsernameNotFoundException("User not found: " + email);
                    }
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } catch (JwtException | IllegalArgumentException | UsernameNotFoundException | BadCredentialsException exception) {
                    SecurityContextHolder.clearContext();
                    authenticationEntryPoint.commence(
                            request,
                            response,
                            new BadCredentialsException("Invalid token", exception)
                    );
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
