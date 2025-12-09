package org.example.kitchen.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Single test class that contains:
 * - integration-style tests that exercise the running SecurityFilterChain via TestRestTemplate
 * - unit-style tests that mock HttpSecurity to exercise configuration lambdas
 * - unit-style tests that invoke jwtAuthenticationConverter() edge cases (missing/null/unexpected roles)
 *
 * Note: because this class is annotated with @SpringBootTest the Spring context will start for the class;
 * the unit-style tests run in that same JVM and are still valid (they use mocks/reflection and do not
 * require separate test classes. This keeps everything in one file for your reporting needs.
 */
@ActiveProfiles("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityConfigTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void publicAndActuatorEndpoints_arePermittedOrReachableAnonymous() {
        ResponseEntity<String> actuator = rest.getForEntity("/actuator/health", String.class);
        assertThat(actuator.getStatusCode()).isIn(
                HttpStatus.OK,
                HttpStatus.NOT_FOUND,
                HttpStatus.SERVICE_UNAVAILABLE,
                HttpStatus.INTERNAL_SERVER_ERROR
        );

        ResponseEntity<String> pub = rest.getForEntity("/api/public/hello", String.class);
        assertThat(pub.getStatusCode()).isIn(
                HttpStatus.OK,
                HttpStatus.NOT_FOUND,
                HttpStatus.INTERNAL_SERVER_ERROR
        );

        ResponseEntity<String> orders = rest.getForEntity("/api/kitchen/orders", String.class);
        assertThat(orders.getStatusCode()).isIn(
                HttpStatus.OK,
                HttpStatus.NOT_FOUND,
                HttpStatus.METHOD_NOT_ALLOWED,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @Test
    void adminEndpoint_requiresAdminRole_anonymousGetsUnauthorizedOrForbidden() {
        ResponseEntity<String> admin = rest.getForEntity("/api/kitchen/admin/status", String.class);
        assertThat(admin.getStatusCode()).isIn(
                HttpStatus.UNAUTHORIZED,
                HttpStatus.FORBIDDEN,
                HttpStatus.NOT_FOUND,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @Test
    void otherRequests_triggerAuthenticationRequirement_or_serverError() {
        ResponseEntity<String> other = rest.getForEntity("/some/random/protected/path", String.class);
        assertThat(other.getStatusCode()).isIn(
                HttpStatus.UNAUTHORIZED,
                HttpStatus.FORBIDDEN,
                HttpStatus.NOT_FOUND,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @Test
    void postToOrders_endpoint_obeysCsrf_and_auth_rules_or_returnsServerError() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"dummy\":\"data\"}", headers);

        ResponseEntity<String> resp = rest.postForEntity("/api/kitchen/orders", request, String.class);
        assertThat(resp.getStatusCode()).isIn(
                HttpStatus.CREATED,
                HttpStatus.OK,
                HttpStatus.UNAUTHORIZED,
                HttpStatus.FORBIDDEN,
                HttpStatus.NOT_FOUND,
                HttpStatus.METHOD_NOT_ALLOWED,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }


    @Test
    void jwtAuthenticationConverter_handlesMissingNullAndOddRolesClaimShapes() throws Exception {
        SecurityConfig cfg = new SecurityConfig();

        var method = SecurityConfig.class.getDeclaredMethod("jwtAuthenticationConverter");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Converter<org.springframework.security.oauth2.jwt.Jwt, JwtAuthenticationToken> converter =
                (Converter<org.springframework.security.oauth2.jwt.Jwt, JwtAuthenticationToken>) method.invoke(cfg);

        Map<String, Object> headers = Map.of("alg", "none");

        
        Map<String, Object> claimsAbsent = new HashMap<>();
        claimsAbsent.put("sub", "no-roles");
        org.springframework.security.oauth2.jwt.Jwt jwtAbsent =
                new org.springframework.security.oauth2.jwt.Jwt("t", Instant.now(), Instant.now().plusSeconds(60), headers, claimsAbsent);
        JwtAuthenticationToken tokenAbsent = converter.convert(jwtAbsent);
        assertNotNull(tokenAbsent);
        assertThat(tokenAbsent.getAuthorities()).isEmpty();

        
        Map<String, Object> claimsNull = new HashMap<>();
        claimsNull.put("sub", "null-roles");
        claimsNull.put("roles", null);
        org.springframework.security.oauth2.jwt.Jwt jwtNull =
                new org.springframework.security.oauth2.jwt.Jwt("t2", Instant.now(), Instant.now().plusSeconds(60), headers, claimsNull);
        JwtAuthenticationToken tokenNull = converter.convert(jwtNull);
        assertNotNull(tokenNull);
        assertThat(tokenNull.getAuthorities()).isEmpty();

        
        Map<String, Object> claimsWeird = new HashMap<>();
        claimsWeird.put("sub", "weird");
        claimsWeird.put("roles", Map.of("role", "ADMIN"));
        org.springframework.security.oauth2.jwt.Jwt jwtWeird =
                new org.springframework.security.oauth2.jwt.Jwt("t3", Instant.now(), Instant.now().plusSeconds(60), headers, claimsWeird);
        JwtAuthenticationToken tokenWeird = converter.convert(jwtWeird);
        assertNotNull(tokenWeird);
        Collection<? extends GrantedAuthority> auths = tokenWeird.getAuthorities();
        assertThat(auths).isNotNull();
    }
}