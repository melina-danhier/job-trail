package com.melina.jobtrail.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "test-only-secret-key-with-at-least-32-bytes";
    private static final String ISSUER = "jobtrail-test";

    @Test
    @DisplayName("Generated token can be successfully validated and email extracted")
    void generatedTokenCanBeValidatedByConfiguredIssuerAndKey() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, Duration.ofHours(1), ISSUER);

        String token = jwtUtil.generateToken("user@example.com");

        assertEquals("user@example.com", jwtUtil.validateTokenAndExtractEmail(token));
    }

    @Test
    @DisplayName("Weak signing key (< 256 bits) is rejected at startup")
    void weakSigningKeyIsRejectedAtStartup() {
        assertThrows(
                WeakKeyException.class,
                () -> new JwtUtil("too-short", Duration.ofHours(1), ISSUER)
        );
    }

    @Test
    @DisplayName("Zero or negative token lifetime is rejected at startup")
    void invalidTokenLifetimeIsRejectedAtStartup() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtUtil(SECRET, Duration.ZERO, ISSUER)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtUtil(SECRET, Duration.ofMinutes(-5), ISSUER)
        );
    }

    @Test
    @DisplayName("Null or blank issuer is rejected at startup")
    void invalidIssuerIsRejectedAtStartup() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtUtil(SECRET, Duration.ofHours(1), null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtUtil(SECRET, Duration.ofHours(1), "   ")
        );
    }

    @Test
    @DisplayName("Expired JWT token is rejected with ExpiredJwtException")
    void expiredToken_throwsExpiredJwtException() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, Duration.ofHours(1), ISSUER);
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date past = new Date(now.getTime() - 60000);

        String expiredToken = Jwts.builder()
                .subject("expired@example.com")
                .issuer(ISSUER)
                .issuedAt(new Date(past.getTime() - 60000))
                .expiration(past)
                .signWith(key)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> jwtUtil.validateTokenAndExtractEmail(expiredToken));
    }

    @Test
    @DisplayName("Malformed JWT token string is rejected with MalformedJwtException")
    void malformedToken_throwsMalformedJwtException() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, Duration.ofHours(1), ISSUER);

        assertThrows(MalformedJwtException.class, () -> jwtUtil.validateTokenAndExtractEmail("not.a.valid.jwt.token"));
        assertThrows(MalformedJwtException.class, () -> jwtUtil.validateTokenAndExtractEmail("abc123xyz"));
    }

    @Test
    @DisplayName("Token signed with different secret key is rejected with SecurityException")
    void tokenWithDifferentKey_throwsSecurityException() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, Duration.ofHours(1), ISSUER);
        SecretKey foreignKey = Keys.hmacShaKeyFor("another-secret-key-that-is-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8));

        String tokenFromUntrustedSource = Jwts.builder()
                .subject("attacker@example.com")
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(foreignKey)
                .compact();

        assertThrows(SecurityException.class, () -> jwtUtil.validateTokenAndExtractEmail(tokenFromUntrustedSource));
    }

    @Test
    @DisplayName("Token with incorrect issuer is rejected with IncorrectClaimException")
    void tokenWithWrongIssuer_throwsIncorrectClaimException() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, Duration.ofHours(1), ISSUER);
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        String tokenWithWrongIssuer = Jwts.builder()
                .subject("user@example.com")
                .issuer("some-other-service")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key)
                .compact();

        assertThrows(IncorrectClaimException.class, () -> jwtUtil.validateTokenAndExtractEmail(tokenWithWrongIssuer));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Null, empty or blank token is rejected with IllegalArgumentException")
    void emptyOrNullToken_throwsIllegalArgumentException(String emptyToken) {
        JwtUtil jwtUtil = new JwtUtil(SECRET, Duration.ofHours(1), ISSUER);

        assertThrows(IllegalArgumentException.class, () -> jwtUtil.validateTokenAndExtractEmail(emptyToken));
    }
}
