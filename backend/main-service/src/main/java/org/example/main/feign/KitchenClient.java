package org.example.main.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.List;

@FeignClient(name = "kitchen-svc", url = "${feign.kitchen.url:http://localhost:8081}")
public interface KitchenClient {

    @PostMapping("/api/kitchen/orders")
    KitchenOrderResponse createKitchenOrder(@RequestBody KitchenOrderRequest request);

    @GetMapping("/api/kitchen/orders/{id}")
    KitchenOrderResponse getKitchenOrder(@PathVariable("id") UUID id);

    @PutMapping("/api/kitchen/orders/{id}/status")
    KitchenOrderResponse updateKitchenOrderStatus(@PathVariable("id") UUID id, @RequestBody KitchenOrderStatusUpdate request);

    class KitchenOrderRequest {
        public UUID sourceOrderId;
        public Integer tableNumber;
        public String customerName;
        public List<KitchenOrderItem> items;
    }

    class KitchenOrderItem {
        public UUID menuItemId;
        public String menuItemName;
        public Integer quantity;
    }

    class KitchenOrderResponse {
        public UUID id;
        public UUID sourceOrderId;
        public String status;
        public String note;
    }

    class KitchenOrderStatusUpdate {
        public String status;
    }
}