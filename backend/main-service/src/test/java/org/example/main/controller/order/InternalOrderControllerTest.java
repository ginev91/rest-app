package org.example.main.controller.order;

import feign.FeignException;
import org.example.main.controller.kitchen.InternalKitchenController;
import org.example.main.feign.KitchenClient;
import org.example.main.model.order.OrderEntity;
import org.example.main.repository.order.OrderRepository;
import org.example.main.service.order.OrderService;
import org.example.main.dto.kitchen.KitchenStatusDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalOrderControllerTest {

    @Mock
    OrderService orderService;

    @Mock
    KitchenClient kitchenClient;

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    InternalKitchenController ctrl;

    @Test
    void kitchenReady_callsService_and_returnsOk() {
        UUID orderId = UUID.randomUUID();
        UUID kitchenOrderId = UUID.randomUUID();

        ResponseEntity<Void> resp = ctrl.kitchenReady(orderId, kitchenOrderId);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        verify(orderService).updateKitchenStatus(orderId, "READY", kitchenOrderId);
    }

    @Test
    void kitchenReady_handlesServiceException_and_returns500() {
        UUID orderId = UUID.randomUUID();
        doThrow(new RuntimeException("boom")).when(orderService).updateKitchenStatus(eq(orderId), eq("READY"), any());

        ResponseEntity<Void> resp = ctrl.kitchenReady(orderId, null);

        assertThat(resp.getStatusCode().value()).isEqualTo(500);
        verify(orderService).updateKitchenStatus(orderId, "READY", null);
    }

    @Test
    void ping_returnsPong() {
        ResponseEntity<String> resp = ctrl.ping();
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isEqualTo("pong");
    }

    @Test
    void updateKitchenOrderStatus_success_forwards_and_updatesMainOrder() {
        UUID kitchenOrderId = UUID.randomUUID();
        KitchenClient.KitchenOrderResponse resp = new KitchenClient.KitchenOrderResponse();
        resp.id = UUID.randomUUID();
        resp.status = "READY";

        when(kitchenClient.updateKitchenOrderStatus(eq(kitchenOrderId), any()))
                .thenReturn(resp);

        OrderEntity oe = new OrderEntity();
        oe.setKitchenStatus("OLD");
        when(orderRepository.findByKitchenOrderId(kitchenOrderId)).thenReturn(Optional.of(oe));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        KitchenStatusDto dto = new KitchenStatusDto();
        dto.setStatus("READY");

        ResponseEntity<?> r = ctrl.updateKitchenOrderStatus(kitchenOrderId, dto);

        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) r.getBody();
        assertThat(body).containsEntry("kitchenOrderId", resp.id).containsEntry("status", "READY");

        assertThat(oe.getKitchenStatus()).isEqualTo("READY");
        verify(orderRepository).save(oe);
    }

    @Test
    void updateKitchenOrderStatus_whenFeignThrows_returns502() {
        UUID kitchenOrderId = UUID.randomUUID();
        FeignException fe = mock(FeignException.class);
        when(fe.getMessage()).thenReturn("kitchen service error");
        when(kitchenClient.updateKitchenOrderStatus(eq(kitchenOrderId), any())).thenThrow(fe);

        KitchenStatusDto dto = new KitchenStatusDto();
        dto.setStatus("READY");

        ResponseEntity<?> r = ctrl.updateKitchenOrderStatus(kitchenOrderId, dto);

        assertThat(r.getStatusCode().value()).isEqualTo(502);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) r.getBody();
        assertThat(body).containsKey("error").containsKey("detail");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelKitchenOrder_success_forwards_and_updatesMainOrder() {
        UUID kitchenOrderId = UUID.randomUUID();

        // kitchenClient.cancelKitchenOrder does not return; mock to do nothing
        doNothing().when(kitchenClient).cancelKitchenOrder(kitchenOrderId);

        OrderEntity oe = new OrderEntity();
        oe.setKitchenStatus("OLD");
        when(orderRepository.findByKitchenOrderId(kitchenOrderId)).thenReturn(Optional.of(oe));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> r = ctrl.cancelKitchenOrder(kitchenOrderId);

        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(oe.getKitchenStatus()).isEqualTo("CANCELLED");
        verify(orderRepository).save(oe);
    }

    @Test
    void cancelKitchenOrder_whenFeignThrows_returns502() {
        UUID kitchenOrderId = UUID.randomUUID();
        FeignException fe = mock(FeignException.class);
        when(fe.getMessage()).thenReturn("kitchen cancel error");
        doThrow(fe).when(kitchenClient).cancelKitchenOrder(kitchenOrderId);

        ResponseEntity<?> r = ctrl.cancelKitchenOrder(kitchenOrderId);

        assertThat(r.getStatusCode().value()).isEqualTo(502);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) r.getBody();
        assertThat(body).containsKey("error").containsKey("detail");
    }
}