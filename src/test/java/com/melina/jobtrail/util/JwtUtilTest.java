package com.melina.jobtrail.util;

import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    @Test
    void generatedTokenCanBeValidatedByConfiguredIssuerAndKey() {
        JwtUtil jwtUtil = new JwtUtil(
                "test-only-secret-key-with-at-least-32-bytes",
                Duration.ofHours(1),
                "jobtrail-test"
        );

        String token = jwtUtil.generateToken("user@example.com");

        assertEquals("user@example.com", jwtUtil.validateTokenAndExtractEmail(token));
    }

    @Test
    void weakSigningKeyIsRejectedAtStartup() {
        assertThrows(
                WeakKeyException.class,
                () -> new JwtUtil("too-short", Duration.ofHours(1), "jobtrail-test")
        );
    }

    @Test
    void invalidTokenLifetimeIsRejectedAtStartup() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtUtil(
                        "test-only-secret-key-with-at-least-32-bytes",
                        Duration.ZERO,
                        "jobtrail-test"
                )
        );
    }
}
