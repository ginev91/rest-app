package org.example.main.controller.kitchen;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.example.main.feign.KitchenClient;
import org.example.main.model.order.OrderEntity;
import org.example.main.repository.order.OrderRepository;
import org.example.main.service.order.OrderService;
import org.example.main.dto.kitchen.KitchenStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Validated
public class InternalKitchenController {

    private static final Logger log = LoggerFactory.getLogger(InternalKitchenController.class);

    private final OrderService orderService;
    private final KitchenClient kitchenClient;
    private final OrderRepository orderRepository;

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

    @PutMapping("/orders/{kitchenOrderId}/status")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<?> updateKitchenOrderStatus(
            @PathVariable UUID kitchenOrderId,
            @Valid @RequestBody KitchenStatusDto body) {
        try {
            log.info("Forwarding kitchen status update to kitchen-svc for kitchenOrderId={} status={}", kitchenOrderId, body.getStatus());
            KitchenClient.KitchenOrderResponse resp = kitchenClient.updateKitchenOrderStatus(kitchenOrderId, new KitchenClient.KitchenOrderStatusUpdate(body.getStatus()));

            Optional<OrderEntity> orderOpt = orderRepository.findByKitchenOrderId(kitchenOrderId);
            orderOpt.ifPresent(order -> {
                order.setKitchenStatus(resp.status);
                orderRepository.save(order);
            });

            return ResponseEntity.ok(Map.of("kitchenOrderId", resp.id, "status", resp.status));
        } catch (FeignException fe) {
            log.error("Feign error while updating kitchen order status: {}", fe.status(), fe);
            return ResponseEntity.status(502).body(Map.of("error", "Kitchen service unavailable", "detail", fe.getMessage()));
        } catch (Exception ex) {
            log.error("Error while updating kitchen order status", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Internal error"));
        }
    }

    @PostMapping("/orders/{kitchenOrderId}/cancel")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<?> cancelKitchenOrder(@PathVariable UUID kitchenOrderId) {
        try {
            log.info("Forwarding cancel to kitchen-svc for kitchenOrderId={}", kitchenOrderId);
            kitchenClient.cancelKitchenOrder(kitchenOrderId);

            Optional<OrderEntity> orderOpt = orderRepository.findByKitchenOrderId(kitchenOrderId);
            orderOpt.ifPresent(order -> {
                order.setKitchenStatus("CANCELLED");
                orderRepository.save(order);
            });

            return ResponseEntity.ok().build();
        } catch (FeignException fe) {
            log.error("Feign error while cancelling kitchen order: {}", fe.status(), fe);
            return ResponseEntity.status(502).body(Map.of("error", "Kitchen service unavailable", "detail", fe.getMessage()));
        } catch (Exception ex) {
            log.error("Error while cancelling kitchen order", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Internal error"));
        }
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}