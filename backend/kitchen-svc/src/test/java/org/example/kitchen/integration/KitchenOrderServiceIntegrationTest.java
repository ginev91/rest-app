package org.example.kitchen.integration;

import org.example.kitchen.exception.KitchenOrderOperationException;
import org.example.kitchen.model.KitchenOrder;
import org.example.kitchen.model.enums.KitchenOrderStatus;
import org.example.kitchen.repository.KitchenOrderRepository;
import org.example.kitchen.service.KitchenOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for KitchenOrderService that exercise repository + service logic.
 * - Avoids createOrder() since it schedules background tasks; instead saves entities directly in the repository
 *   and exercises updateStatus() and cancelOrder() rules.
 */
@SpringBootTest(properties = {
        "kitchen.callback.enabled=false",
        "kitchen.prep.enabled=false",
        "kitchen.prep.min-seconds=1",
        "kitchen.prep.max-seconds=1"
})
@ActiveProfiles("test")
class KitchenOrderServiceIntegrationTest {

    @Autowired
    private KitchenOrderService service;

    @Autowired
    private KitchenOrderRepository repository;

    private KitchenOrder entity;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        // Let JPA generate the id (don't assign manually) and persist immediately
        entity = KitchenOrder.builder()
                .orderId(java.util.UUID.randomUUID())
                .itemsJson("[]")
                .status(KitchenOrderStatus.PREPARING)
                .createdAt(Instant.now())
                .build();

        // Persist and flush to DB so the test sees a fully managed instance
        entity = repository.saveAndFlush(entity);
    }

    @Test
    void updateStatus_allowsValidTransitions_and_rejectsInvalid() {
        // valid transition PREPARING -> IN_PROGRESS
        KitchenOrder updated = service.updateStatus(entity.getId(), KitchenOrderStatus.IN_PROGRESS);
        assertThat(updated.getStatus()).isEqualTo(KitchenOrderStatus.IN_PROGRESS);

        // invalid transition IN_PROGRESS -> NEW (not allowed)
        assertThatThrownBy(() -> service.updateStatus(entity.getId(), KitchenOrderStatus.NEW))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void cancelOrder_allowsAndRejectsBasedOnCurrentState() {
        // Cancel when PREPARING - should succeed
        service.cancelOrder(entity.getId());
        KitchenOrder after = repository.findById(entity.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(KitchenOrderStatus.CANCELLED);

        // Trying to cancel again should throw
        assertThatThrownBy(() -> service.cancelOrder(entity.getId()))
                .isInstanceOf(KitchenOrderOperationException.class)
                .hasMessageContaining("already cancelled");
    }
}