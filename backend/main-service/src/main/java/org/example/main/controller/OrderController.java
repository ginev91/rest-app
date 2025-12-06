package org.example.main.controller;

import jakarta.validation.Valid;
import org.example.main.dto.request.OrderRequestDto;
import org.example.main.dto.response.OrderDetailsResponseDto;
import org.example.main.dto.response.OrderResponseDto;
import org.example.main.model.OrderEntity;
import org.example.main.model.enums.OrderStatus;
import org.example.main.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
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

    /**
     * GET /api/orders?userId={uuid} - list orders for a user
     * GET /api/orders?tableId={uuid} - list orders for the table
     */
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

    /**
     * GET /api/orders/{id} - full details
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailsResponseDto> getOrder(@PathVariable("id") UUID id) {
        OrderDetailsResponseDto dto = orderService.getOrderDetails(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * POST /api/orders - create order (authenticated users)
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto request) {
        OrderResponseDto resp = orderService.createOrder(request);
        return ResponseEntity.status(201).body(resp);
    }

    /**
     * PUT /api/orders/{id}/status - change status (restricted to ROLE_WAITER or ROLE_ADMIN)
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('WAITER','ADMIN')")
    public ResponseEntity<Void> updateStatus(@PathVariable("id") UUID id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        orderService.getOrderSummary(id); // throws 404 if not found
        // For now, call cancel/order-specific methods or set status via a new service method
        // We'll reuse existing service API: if status == "cancelled" -> cancelOrder, else call a generic update (implement as needed)
        if ("cancelled".equalsIgnoreCase(status)) {
            orderService.cancelOrder(id);
        } else {
            // Implement a generic updateStatus method on service if you want persisted transitions
            // For now set via cancel/check methods or create updateStatus in IOrderService
            // Fallback: call cancel / callWaiter or similar as appropriate
            // TODO: replace with orderService.updateStatus(id, status);
            // Example:
            // orderService.updateStatus(id, status);
            throw new UnsupportedOperationException("Status update operation not yet implemented in service");
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/orders/{id} - cancel order (restricted: owner OR role)
     * For simplicity require WAITER/ADMIN for cancellation in this example. Adjust if you want to allow owner cancels.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('WAITER','ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable("id") UUID id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    public ResponseEntity<OrderResponseDto> getActiveOrder(@RequestParam UUID userId) {
        return orderService.getActiveOrderForUser(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}