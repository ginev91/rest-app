package org.example.main.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * JwtAuthenticationFilter that uses UserDetailsService (interface) to obtain a UserDetails instance.
 * This avoids placing JPA entities into the SecurityContext and prevents circular deps.
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
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtUtils.validateToken(token)) {
                    String username = jwtUtils.getUsernameFromToken(token);
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        try {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            if (userDetails != null) {
                                var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                                SecurityContextHolder.getContext().setAuthentication(auth);
                            }
                        } catch (UsernameNotFoundException unfe) {
                            log.debug("User not found for username from token: {}", username);
                        }
                    }
                } else {
                    log.debug("Invalid or expired JWT for request {}", request.getRequestURI());
                }
            }
        } catch (Exception ex) {
            // don't rethrow - continue as anonymous
            log.debug("JWT processing failed (continuing anonymous): {}", ex.getMessage());
        }

        chain.doFilter(request, response);
    }
}