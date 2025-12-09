package org.example.main.controller;

import org.example.main.dto.request.OrderRequestDto;
import org.example.main.dto.response.OrderDetailsResponseDto;
import org.example.main.dto.response.OrderResponseDto;
import org.example.main.service.IOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    IOrderService orderService;

    @Test
    void listOrders_withUserId_delegatesToService() {
        UUID userId = UUID.randomUUID();
        OrderResponseDto r = new OrderResponseDto();
        when(orderService.getOrdersForUser(userId)).thenReturn(List.of(r));

        OrderController ctrl = new OrderController(orderService);
        ResponseEntity<List<OrderResponseDto>> resp = ctrl.listOrders(userId, null);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).containsExactly(r);
        verify(orderService).getOrdersForUser(userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void listOrders_withTableId_delegatesToService() {
        UUID tableId = UUID.randomUUID();
        OrderResponseDto r = new OrderResponseDto();
        when(orderService.getOrdersForTable(tableId)).thenReturn(List.of(r));

        OrderController ctrl = new OrderController(orderService);
        ResponseEntity<List<OrderResponseDto>> resp = ctrl.listOrders(null, tableId);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).containsExactly(r);
        verify(orderService).getOrdersForTable(tableId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void listOrders_noFilters_returnsAll() {
        OrderResponseDto r1 = new OrderResponseDto();
        OrderResponseDto r2 = new OrderResponseDto();
        when(orderService.getAllOrders()).thenReturn(List.of(r1, r2));

        OrderController ctrl = new OrderController(orderService);
        ResponseEntity<List<OrderResponseDto>> resp = ctrl.listOrders(null, null);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).containsExactly(r1, r2);
        verify(orderService).getAllOrders();
    }

    @Test
    void getOrder_delegatesToService_and_returnsDto() {
        UUID id = UUID.randomUUID();
        OrderDetailsResponseDto dto = new OrderDetailsResponseDto();
        when(orderService.getOrderDetails(id)).thenReturn(dto);

        OrderController ctrl = new OrderController(orderService);
        ResponseEntity<OrderDetailsResponseDto> resp = ctrl.getOrder(id);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isSameAs(dto);
        verify(orderService).getOrderDetails(id);
    }

    @Test
    void createOrder_delegatesToService_returnsCreated() {
        OrderRequestDto req = new OrderRequestDto();
        OrderResponseDto out = new OrderResponseDto();
        when(orderService.createOrder(req)).thenReturn(out);

        OrderController ctrl = new OrderController(orderService);
        ResponseEntity<OrderResponseDto> resp = ctrl.createOrder(req);

        assertThat(resp.getStatusCodeValue()).isEqualTo(201);
        assertThat(resp.getBody()).isSameAs(out);
        verify(orderService).createOrder(req);
    }

    @Test
    void updateStatus_withoutStatus_returnsBadRequest() {
        UUID id = UUID.randomUUID();
        OrderController ctrl = new OrderController(orderService);

        Map<String, String> body = Map.of();
        ResponseEntity<Void> resp = ctrl.updateStatus(id, body);

        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
        verifyNoInteractions(orderService);
    }

    @Test
    void updateStatus_cancelled_callsCancelOrder_and_returnsNoContent() {
        UUID id = UUID.randomUUID();
        OrderController ctrl = new OrderController(orderService);

        when(orderService.getOrderSummary(id)).thenReturn(new OrderResponseDto());

        Map<String, String> body = Map.of("status", "cancelled");
        ResponseEntity<Void> resp = ctrl.updateStatus(id, body);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        verify(orderService).getOrderSummary(id);
        verify(orderService).cancelOrder(id);
    }

    @Test
    void updateStatus_other_throwsUnsupportedOperation() {
        UUID id = UUID.randomUUID();
        OrderController ctrl = new OrderController(orderService);

        when(orderService.getOrderSummary(id)).thenReturn(new OrderResponseDto());

        Map<String, String> body = Map.of("status", "ready");
        assertThatThrownBy(() -> ctrl.updateStatus(id, body))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not yet implemented");

        verify(orderService).getOrderSummary(id);
        verify(orderService, never()).cancelOrder(any());
    }

    @Test
    void deleteOrder_callsCancel_and_returnsNoContent() {
        UUID id = UUID.randomUUID();
        OrderController ctrl = new OrderController(orderService);
        ResponseEntity<Void> resp = ctrl.deleteOrder(id);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        verify(orderService).cancelOrder(id);
    }

    @Test
    void getActiveOrder_present_and_absent_behaviour() {
        UUID userId = UUID.randomUUID();
        OrderResponseDto dto = new OrderResponseDto();

        when(orderService.getActiveOrderForUser(userId)).thenReturn(Optional.of(dto));
        OrderController ctrl = new OrderController(orderService);

        ResponseEntity<OrderResponseDto> resp1 = ctrl.getActiveOrder(userId);
        assertThat(resp1.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp1.getBody()).isSameAs(dto);

        when(orderService.getActiveOrderForUser(userId)).thenReturn(Optional.empty());
        ResponseEntity<OrderResponseDto> resp2 = ctrl.getActiveOrder(userId);
        assertThat(resp2.getStatusCodeValue()).isEqualTo(204);
    }
}