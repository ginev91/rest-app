package org.example.kitchen.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.kitchen.dto.request.CreateKitchenOrderRequest;
import org.example.kitchen.dto.request.UpdateStatusRequest;
import org.example.kitchen.dto.response.KitchenOrderResponse;
import org.example.kitchen.mapper.KitchenOrderMapper;
import org.example.kitchen.model.KitchenOrder;
import org.example.kitchen.service.IKitchenOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/kitchen/orders")
@RequiredArgsConstructor
public class KitchenOrderController {

    private final IKitchenOrderService service;

    @PostMapping
    public ResponseEntity<KitchenOrderResponse> create(@Valid @RequestBody CreateKitchenOrderRequest req) {
        KitchenOrder created = service.createOrder(req.getOrderId(), req.getItemsJson());
        KitchenOrderResponse resp = KitchenOrderMapper.toResponse(created);
        return ResponseEntity.created(URI.create("/api/kitchen/orders/" + created.getId())).body(resp);
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<List<KitchenOrderResponse>> getByOrder(@PathVariable("orderId") UUID orderId) {
        List<KitchenOrderResponse> list = service.findByOrderId(orderId)
                .stream().map(KitchenOrderMapper::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<KitchenOrderResponse> updateStatus(@PathVariable("id") UUID id, @Valid @RequestBody UpdateStatusRequest req) {
        KitchenOrder updated = service.updateStatus(id, req.getStatus());
        return ResponseEntity.ok(KitchenOrderMapper.toResponse(updated));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable("id") UUID id) {
        service.cancelOrder(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}