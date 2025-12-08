package org.example.main.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Utility for JWT generation/validation.
 * Token retrieval is cookie-first (cookie name "access_token").
 *
 * This version includes roles in the generated token under the "roles" claim.
 */
@Component
public class JwtUtils {

    private final Key signingKey;
    private final long jwtExpirationMs;

    public JwtUtils(
            @Value("${jwt.secret:${app.jwtSecret:${APP_JWT_SECRET:}}}") String jwtSecret,
            @Value("${jwt.expiration-ms:${app.jwtExpirationMs:${APP_JWT_EXPIRATION_MS:3600000}}}") long jwtExpirationMs
    ) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT secret not configured. Set property jwt.secret or app.jwtSecret or env APP_JWT_SECRET"
            );
        }

        if (jwtSecret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters for HS256");
        }

        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Generate token for a username with roles included in the "roles" claim.
     */
    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);
        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256);

        if (roles != null && !roles.isEmpty()) {
            builder.claim("roles", roles);
        }

        return builder.compact();
    }

    /**
     * Backward-compatible token generation without roles (delegates).
     */
    public String generateToken(String username) {
        return generateToken(username, List.of());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            return false;
        } catch (JwtException | IllegalArgumentException ex) {
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

    /**
     * Retrieve token from the HttpServletRequest.
     * Prefers cookie-based token (cookie name "access_token").
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        if (request == null) return null;

        try {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("access_token".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                        return c.getValue().trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        String param = request.getParameter("access_token");
        if (param != null && !param.isBlank()) {
            return param.trim();
        }

        return null;
    }

    public Authentication buildAuthentication(UserDetails userDetails, HttpServletRequest request) {
        if (userDetails == null) return null;

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        try {
            String token = getTokenFromRequest(request);
            if (token != null && !token.isBlank() && validateToken(token)) {
                Claims claims = parseClaims(token);
                Object rolesClaim = claims.get("roles");
                if (rolesClaim instanceof List<?>) {
                    for (Object r : (List<?>) rolesClaim) {
                        if (r != null) authorities.add(new SimpleGrantedAuthority(r.toString()));
                    }
                } else if (rolesClaim instanceof String) {
                    for (String p : ((String) rolesClaim).split(",")) {
                        if (!p.isBlank()) authorities.add(new SimpleGrantedAuthority(p.trim()));
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (authorities.isEmpty()) {
            userDetails.getAuthorities().forEach(a -> authorities.add(new SimpleGrantedAuthority(a.getAuthority())));
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        if (request != null) {
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        }
        return auth;
    }

    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }
}