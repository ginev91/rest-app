package org.example.main.config;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(TestSecurityConfig.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults()); 
        return http.build();


    }

    
    
    
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public Filter headerDumpFilter() {
        return (servletRequest, servletResponse, chain) -> {
            HttpServletRequest req = (HttpServletRequest) servletRequest;
            HttpServletResponse res = (HttpServletResponse) servletResponse;

            String authHeader = req.getHeader("Authorization");
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            log.info("HeaderDumpFilter: Authorization present={} ; Authentication={}",
                    authHeader != null, auth != null ? auth.getClass().getSimpleName() + "[" + auth.getName() + "]" : null);

            chain.doFilter(req, res);
        };
    }
}