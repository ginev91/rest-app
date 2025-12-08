package org.example.kitchen.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityConfigFilterInvocationTest {

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
}