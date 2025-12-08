package org.example.main.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Provides a simple in-memory user for tests.
 */
@TestConfiguration
public class TestAuthConfig {

    @Bean
    @Primary
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails employee = User.withDefaultPasswordEncoder() // OK for tests only
                .username("employee@test.bg")
                .password("emppass")
                .roles("EMPLOYEE")
                .build();
        return new InMemoryUserDetailsManager(employee);
    }
}