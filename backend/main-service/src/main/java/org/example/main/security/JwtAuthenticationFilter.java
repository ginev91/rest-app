package org.example.main.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;

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

        
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/api/internal/")) {
            log.debug("Skipping JWT processing for internal request: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = jwtUtils.getTokenFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                
                if (!jwtUtils.validateToken(jwt)) {
                    log.debug("JWT present but invalid/expired for request {}: {}", path, jwt);
                    handleAuthFailure(request, response, "invalid_or_expired_token");
                    return;
                }

                String username = jwtUtils.getUsernameFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                var auth = jwtUtils.buildAuthentication(userDetails, request);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JWT authentication succeeded for user {}", username);
            } else {
                log.trace("No JWT found in request {}", path);
            }

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.info("JWT authentication failed: {}", ex.getMessage(), ex);
            handleAuthFailure(request, response, "invalid_or_expired_token");
        }
    }

    private void handleAuthFailure(HttpServletRequest request, HttpServletResponse response, String reason) throws IOException {
        SecurityContextHolder.clearContext();
        try {
            HttpSession s = request.getSession(false);
            if (s != null) {
                s.invalidate();
            }
        } catch (Exception ignore) {
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