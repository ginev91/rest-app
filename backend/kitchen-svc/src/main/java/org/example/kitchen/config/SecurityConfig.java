package org.example.kitchen.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Security configuration for kitchen service (resource server).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${security.jwt.secret:}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/kitchen/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()) // uses our converter
                        )
                );

        return http.build();
    }

    /**
     * Build a Converter<Jwt, AbstractAuthenticationToken> that:
     *  - extracts authorities from a claim using JwtGrantedAuthoritiesConverter
     *  - post-processes/massages the authorities if necessary (e.g. ensure ROLE_ prefix or uppercase)
     *
     * Adjust setAuthoritiesClaimName(...) if your JWT uses a different claim name for roles.
     */
    private Converter<org.springframework.security.oauth2.jwt.Jwt, JwtAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles"); // change if claim is different
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_"); // will produce ROLE_ADMIN, etc.

        // Wrap the granted-authorities converter into a Converter<Jwt, JwtAuthenticationToken>
        return jwt -> {
            Collection<GrantedAuthority> authorities = grantedAuthoritiesConverter.convert(jwt);
            // Optional post-processing: normalize or remap authority names here
            Collection<GrantedAuthority> mapped = authorities.stream()
                    .map(a -> (GrantedAuthority) () -> a.getAuthority().trim()) // identity mapping; replace if needed
                    .collect(Collectors.toList());

            // Create a JwtAuthenticationToken with the mapped authorities
            return new JwtAuthenticationToken(jwt, mapped);
        };
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            // assume jwk-set-uri is configured in properties; otherwise change to actual URI
            return NimbusJwtDecoder.withJwkSetUri("https://example.com/.well-known/jwks.json").build();
        }
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}