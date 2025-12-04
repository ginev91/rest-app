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

    public String getTokenFromRequest(HttpServletRequest request) {
        if (request == null) return null;

        // 1) Authorization header
        String header = request.getHeader("Authorization");
        if (header != null && !header.isBlank()) {
            header = header.trim();
            if (header.length() > 7 && header.substring(0, 7).equalsIgnoreCase("Bearer ")) {
                return header.substring(7).trim();
            }
        }

        // 2) query parameter or form param
        String param = request.getParameter("access_token");
        if (param != null && !param.isBlank()) {
            return param.trim();
        }

        // 3) cookie (optional)
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
            // ignore cookie parsing issues
        }

        return null;
    }

    public Authentication buildAuthentication(UserDetails userDetails, HttpServletRequest request) {
        if (userDetails == null) return null;

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        try {
            String token = getTokenFromRequest(request);
            if (token != null && !token.isBlank() && validateToken(token)) {
                io.jsonwebtoken.Claims claims = parseClaims(token);
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
            // fall back to userDetails authorities below
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
}