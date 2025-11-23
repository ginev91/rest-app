package org.example.main.controller;

import org.example.main.dto.request.OrderRequestDto;
import org.example.main.dto.response.OrderDetailsResponseDto;
import org.example.main.dto.response.OrderResponseDto;
import org.example.main.service.IOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
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

    @GetMapping("/{id}/summary")
    public ResponseEntity<OrderResponseDto> getOrderSummary(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(orderService.getOrderSummary(id));
    }

    // New detailed GET endpoint returning the full order (matches frontend mock)
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
}