package org.example.main.controller;

import org.example.main.dto.request.OrderRequestDto;
import org.example.main.dto.response.OrderResponseDto;
import org.example.main.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<OrderResponseDto> create(@RequestBody @Valid OrderRequestDto dto) {
        UUID id = service.placeOrder(dto);
        OrderResponseDto resp = OrderResponseDto.builder()
                .orderId(id)
                .status("SENT_TO_KITCHEN")
                .build();
        return ResponseEntity.status(201).body(resp);
    }

    @PutMapping("/{id}/call-waiter")
    public ResponseEntity<Map<String, String>> callWaiter(@PathVariable UUID id) {
        service.callWaiter(id);
        return ResponseEntity.ok(Map.of("message", "Waiter called"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        service.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}