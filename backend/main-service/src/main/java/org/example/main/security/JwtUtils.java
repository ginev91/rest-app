package org.example.main.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Utility for JWT generation/validation. Reads properties with fallbacks:
 * - jwt.secret
 * - app.jwtSecret
 * - environment APP_JWT_SECRET
 *
 * and for expiration:
 * - jwt.expiration-ms
 * - app.jwtExpirationMs
 * - environment APP_JWT_EXPIRATION_MS
 */
@Component
public class JwtUtils {

    private final Key signingKey;
    private final long jwtExpirationMs;

    public JwtUtils(
            @Value("${jwt.secret:${app.jwtSecret:${APP_JWT_SECRET:}}}") String jwtSecret,
            @Value("${jwt.expiration-ms:${app.jwtExpirationMs:${APP_JWT_EXPIRATION_MS:3600000}}}") long jwtExpirationMs
    ) {
        // If no secret provided via any mechanism, throw with helpful message
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT secret not configured. Set property jwt.secret or app.jwtSecret or env APP_JWT_SECRET"
            );
        }

        // ensure minimum key size for HMAC-SHA (recommend >= 32 chars)
        if (jwtSecret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters for HS256");
        }

        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Generate JWT for a username (convenience for login/register).
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract username/subject from token.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    /**
     * Validate token signature and expiration. Returns true if valid, false otherwise.
     * Does not throw (caller can react).
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token); // will throw if invalid
            return true;
        } catch (ExpiredJwtException ex) {
            // token expired
            return false;
        } catch (JwtException | IllegalArgumentException ex) {
            // invalid token
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}