package org.example.main.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, defensive JWT filter: on token parse/validation failure it returns a clear 401 JSON
 * response and invalidates the HTTP session (if any). This keeps the change small and avoids
 * adding more infrastructure (no new AuthenticationEntryPoint required).
 *
 * Replace/merge with your existing JwtAuthenticationFilter implementation — keep your token
 * parsing and authentication logic but forward to handleAuthFailure(...) on exceptions.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = jwtUtils.getTokenFromRequest(request);
            if (jwt != null && !jwt.isBlank()) {
                // validate token and set authentication
                if (!jwtUtils.validateToken(jwt)) {
                    // token invalid/expired
                    handleAuthFailure(request, response, "invalid_or_expired_token");
                    return;
                }

                String username = jwtUtils.getUsernameFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                var auth = jwtUtils.buildAuthentication(userDetails, request);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.info("JWT authentication failed: {}", ex.getMessage());
            handleAuthFailure(request, response, "invalid_or_expired_token");
        } finally {
        }
    }

    /**
     * Write a short JSON body and set HTTP 401, then invalidate session (if present).
     * Frontend should treat 401 as a signal to remove stored tokens and redirect to login.
     */
    private void handleAuthFailure(HttpServletRequest request, HttpServletResponse response, String reason) throws IOException {
        // Clear any existing security context just in case
        SecurityContextHolder.clearContext();

        // Invalidate servlet session if present (best-effort)
        try {
            HttpSession s = request.getSession(false);
            if (s != null) {
                s.invalidate();
            }
        } catch (Exception ignore) {
            // ignore
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setHeader("X-Auth-Error", reason);

        String body = "{\"error\":\"unauthenticated\",\"reason\":\"" + reason + "\",\"message\":\"Authentication required or token invalid/expired.\"}";
        try (PrintWriter pw = response.getWriter()) {
            pw.write(body);
            pw.flush();
        }
    }
}