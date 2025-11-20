package org.example.main.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "kitchen-svc", url = "${kitchen.service.url:http://localhost:8081}")
public interface KitchenClient {
    @PostMapping("/api/kitchen/orders")
    void createKitchenOrder(@RequestBody Object kitchenOrder);

    @PutMapping("/api/kitchen/orders/{id}/status")
    void updateKitchenOrderStatus(@PathVariable("id") UUID id, @RequestParam("status") String status);

    @DeleteMapping("/api/kitchen/orders/{id}")
    void cancelKitchenOrder(@PathVariable("id") UUID id);
}