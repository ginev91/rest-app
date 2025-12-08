package org.example.kitchen.integration;

import org.example.kitchen.config.TestSecurityConfig;
import org.example.kitchen.dto.request.CreateKitchenOrderRequest;
import org.example.kitchen.dto.request.UpdateStatusRequest;
import org.example.kitchen.dto.response.KitchenOrderResponse;
import org.example.kitchen.model.enums.KitchenOrderStatus;
import org.example.kitchen.repository.KitchenOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-end test that starts the server and exercises HTTP endpoints.
 * Security is provided by TestSecurityConfig (permitAll). Callback HTTP to main-service is disabled.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "kitchen.callback.enabled=false",
                "kitchen.prep.enabled=false",
                "spring.main.allow-bean-definition-overriding=true"
        })
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class KitchenOrderApiE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private KitchenOrderRepository repository;

    @Test
    @Transactional
    void create_get_update_cancel_and_delete_flow() {
        repository.deleteAll();

        UUID orderId = UUID.randomUUID();
        CreateKitchenOrderRequest createReq = new CreateKitchenOrderRequest();
        createReq.setOrderId(orderId);
        createReq.setItemsJson("[{\"name\":\"pasta\",\"qty\":2}]");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // CREATE -> 201 Created
        ResponseEntity<KitchenOrderResponse> createResp = restTemplate.postForEntity(
                "/api/kitchen/orders", new HttpEntity<>(createReq, headers), KitchenOrderResponse.class);

        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        KitchenOrderResponse created = createResp.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getOrderId()).isEqualTo(orderId);

        // GET by order -> read as a List via ParameterizedTypeReference (safer than array class)
        ResponseEntity<List<KitchenOrderResponse>> listResp;
        try {
            listResp = restTemplate.exchange(
                    "/api/kitchen/orders/by-order/" + orderId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
            );
        } catch (Exception ex) {
            // If deserialization fails, fetch raw String for debugging and fail with the body
            ResponseEntity<String> raw = restTemplate.getForEntity(
                    "/api/kitchen/orders/by-order/" + orderId, String.class);
            String body = raw == null ? "<null response>" : raw.getBody();
            fail("Failed to deserialize list response. Raw response body:\n" + body, ex);
            return;
        }

        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<KitchenOrderResponse> arr = listResp.getBody();
        assertThat(arr).isNotNull();
        assertThat(arr.size()).isGreaterThanOrEqualTo(1);

        UUID kitchenOrderId = created.getId();

        // UPDATE status
        UpdateStatusRequest statusReq = new UpdateStatusRequest();
        statusReq.setStatus(KitchenOrderStatus.IN_PROGRESS);
        ResponseEntity<KitchenOrderResponse> updateResp = restTemplate.exchange(
                "/api/kitchen/orders/" + kitchenOrderId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(statusReq, headers),
                KitchenOrderResponse.class);
        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        KitchenOrderResponse updated = updateResp.getBody();
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(KitchenOrderStatus.IN_PROGRESS);

        // CANCEL (allowed from IN_PROGRESS)
        ResponseEntity<Void> cancelResp = restTemplate.postForEntity(
                "/api/kitchen/orders/" + kitchenOrderId + "/cancel", new HttpEntity<>(headers), Void.class);
        assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // DELETE
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                "/api/kitchen/orders/" + kitchenOrderId, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}