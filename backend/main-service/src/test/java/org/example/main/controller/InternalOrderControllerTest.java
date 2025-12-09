package org.example.main.controller;

import org.example.main.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalOrderControllerTest {

    @Mock
    OrderService orderService;

    @Test
    void kitchenReady_callsService_and_returnsOk() {
        InternalOrderController ctrl = new InternalOrderController(orderService);
        UUID orderId = UUID.randomUUID();
        UUID kitchenOrderId = UUID.randomUUID();

        ResponseEntity<Void> resp = ctrl.kitchenReady(orderId, kitchenOrderId);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        verify(orderService).updateKitchenStatus(orderId, "READY", kitchenOrderId);
    }

    @Test
    void kitchenReady_handlesServiceException_and_returns500() {
        InternalOrderController ctrl = new InternalOrderController(orderService);
        UUID orderId = UUID.randomUUID();

        doThrow(new RuntimeException("boom")).when(orderService).updateKitchenStatus(eq(orderId), eq("READY"), any());

        ResponseEntity<Void> resp = ctrl.kitchenReady(orderId, null);

        assertThat(resp.getStatusCodeValue()).isEqualTo(500);
        verify(orderService).updateKitchenStatus(orderId, "READY", null);
    }

    @Test
    void ping_returnsPong() {
        InternalOrderController ctrl = new InternalOrderController(orderService);
        ResponseEntity<String> resp = ctrl.ping();
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isEqualTo("pong");
    }
}