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

        entity = KitchenOrder.builder()
                .orderId(java.util.UUID.randomUUID())
                .itemsJson("[]")
                .status(KitchenOrderStatus.PREPARING)
                .createdAt(Instant.now())
                .build();

        entity = repository.saveAndFlush(entity);
    }

    @Test
    void updateStatus_allowsValidTransitions_and_rejectsInvalid() {

        KitchenOrder updated = service.updateStatus(entity.getId(), KitchenOrderStatus.IN_PROGRESS);
        assertThat(updated.getStatus()).isEqualTo(KitchenOrderStatus.IN_PROGRESS);

        assertThatThrownBy(() -> service.updateStatus(entity.getId(), KitchenOrderStatus.NEW))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void cancelOrder_allowsAndRejectsBasedOnCurrentState() {
        service.cancelOrder(entity.getId());
        KitchenOrder after = repository.findById(entity.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(KitchenOrderStatus.CANCELLED);

        assertThatThrownBy(() -> service.cancelOrder(entity.getId()))
                .isInstanceOf(KitchenOrderOperationException.class)
                .hasMessageContaining("already cancelled");
    }
}