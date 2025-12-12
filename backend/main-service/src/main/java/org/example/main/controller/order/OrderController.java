package org.example.main.controller.order;

import jakarta.validation.Valid;
import org.example.main.dto.request.order.OrderRequestDto;
import org.example.main.dto.response.order.OrderDetailsResponseDto;
import org.example.main.dto.response.order.OrderResponseDto;
import org.example.main.model.enums.OrderItemStatus;
import org.example.main.model.enums.OrderStatus;
import org.example.main.service.order.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.example.main.service.order.OrderService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    private final IOrderService orderService;

    public OrderController(IOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> listOrders(
            @RequestParam(name = "userId", required = false) UUID userId,
            @RequestParam(name = "tableId", required = false) UUID tableId) {

        List<OrderResponseDto> list;
        if (userId != null) {
            list = orderService.getOrdersForUser(userId);
        } else if (tableId != null) {
            list = orderService.getOrdersForTable(tableId);
        } else {
            list = orderService.getAllOrders();
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailsResponseDto> getOrder(@PathVariable("id") UUID id) {
        OrderDetailsResponseDto dto = orderService.getOrderDetails(id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto request) {
        OrderResponseDto resp = orderService.createOrder(request);
        return ResponseEntity.status(201).body(resp);
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable("id") UUID id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    public ResponseEntity<OrderResponseDto> getActiveOrder(@RequestParam(name = "userId", required = false) UUID userId) {
        return orderService.getActiveOrderForUser(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }


    @PutMapping(value = "/{id}/claim", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('EMPLOYEE')")
    public ResponseEntity<Map<String, Boolean>> claimOrder(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> body) {

        String waiterIdStr = body != null ? body.get("waiterId") : null;
        if (waiterIdStr == null || waiterIdStr.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        UUID waiterId;
        try {
            waiterId = UUID.fromString(waiterIdStr);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }

        boolean claimed = orderService.claimOrder(id, waiterId);
        return ResponseEntity.ok(Collections.singletonMap("claimed", claimed));
    }

    @PutMapping(value = "/{id}/items/{itemId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('EMPLOYEE')")
    public ResponseEntity<Void> updateOrderItemStatus(
            @PathVariable("id") UUID id,
            @PathVariable("itemId") UUID itemId,
            @RequestBody(required = false) Map<String, String> body) {

        if (body == null) {
            return ResponseEntity.badRequest().build();
        }
        String statusLabel = body.get("status");
        if (statusLabel == null || statusLabel.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            OrderItemStatus status = OrderItemStatus.fromLabel(statusLabel);
            orderService.updateOrderItemStatus(id, itemId, status);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('EMPLOYEE')")
    public ResponseEntity<Void> updateStatus(@PathVariable("id") UUID id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            OrderStatus newStatus = OrderService.parseOrderStatus(status);
            orderService.updateOrderStatus(id, newStatus);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ex;
        }
    }
}