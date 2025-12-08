package org.example.main.controller;

import org.example.main.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal")
public class InternalOrderController {

    private static final Logger log = LoggerFactory.getLogger(InternalOrderController.class);
    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders/{orderId}/kitchen-ready")
    public ResponseEntity<Void> kitchenReady(
            @PathVariable("orderId") UUID orderId,
            @RequestParam(name = "kitchenOrderId", required = false) UUID kitchenOrderId) {
        log.info("Received kitchen-ready callback for order {} kitchenOrderId={}", orderId, kitchenOrderId);
        try {
            orderService.updateKitchenStatus(orderId, "READY", kitchenOrderId);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("Failed to update kitchen status for order {}: {}", orderId, ex.getMessage(), ex);
            return ResponseEntity.status(500).build();
        }
    }


    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}