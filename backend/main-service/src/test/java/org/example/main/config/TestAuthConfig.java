package org.example.main.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@TestConfiguration
public class TestAuthConfig {

    @Bean
    @Primary
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails employee = User.withDefaultPasswordEncoder() 
                .username("employee@test.bg")
                .password("emppass")
                .roles("EMPLOYEE")
                .build();
        return new InMemoryUserDetailsManager(employee);
    }
}