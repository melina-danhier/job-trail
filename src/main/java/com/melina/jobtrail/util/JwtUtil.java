package com.melina.jobtrail.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey signingKey;
    private final Duration expiration;
    private final String issuer;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:PT1H}") Duration expiration,
            @Value("${jwt.issuer:jobtrail}") String issuer
    ) {
        if (expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("JWT issuer must not be blank");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.issuer = issuer;
    }

    public String generateToken(String email) {
        Date issuedAt = new Date();
        return Jwts.builder()
                .subject(email)
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiration(new Date(issuedAt.getTime() + expiration.toMillis()))
                .signWith(signingKey)
                .compact();
    }

    public String validateTokenAndExtractEmail(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
