package org.example.main.controller;

import org.example.main.dto.request.OrderRequestDto;
import org.example.main.dto.response.OrderDetailsResponseDto;
import org.example.main.dto.response.OrderResponseDto;
import org.example.main.model.User;
import org.example.main.service.IOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final IOrderService orderService;

    public OrderController(IOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@RequestBody @Valid OrderRequestDto request) {
        UUID id = orderService.placeOrder(request);
        OrderResponseDto resp = OrderResponseDto.builder().orderId(id).status("preparing").build();
        return ResponseEntity.ok(resp);
    }

    @GetMapping
    public ResponseEntity<?> getOrdersByUser(@RequestParam(name = "userId", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body("userId query parameter is required");
        }

        UUID uid;
        try {
            uid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid userId UUID: " + userId);
        }

        try {
            List<OrderResponseDto> orders = orderService.getOrdersForUser(uid);
            return ResponseEntity.ok(orders);
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Error while getting orders for user {}: ", userId, ex);

            String msg = ex.getClass().getName() + ": " + ex.getMessage();
            if (ex.getCause() != null) {
                msg += " Caused by: " + ex.getCause().getClass().getName() + ": " + ex.getCause().getMessage();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<OrderResponseDto> getOrderSummary(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(orderService.getOrderSummary(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailsResponseDto> getOrderDetails(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(orderService.getOrderDetails(id));
    }

    @PostMapping("/{id}/call-waiter")
    public ResponseEntity<Void> callWaiter(@PathVariable("id") UUID id) {
        orderService.callWaiter(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable("id") UUID id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }

    private UUID getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthenticated");
        }

        User user = (User) authentication.getPrincipal();
        return user.getId();
    }
}