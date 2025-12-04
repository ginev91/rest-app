package org.example.main.feign;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.List;

@FeignClient(name = "kitchen-service", url = "${feign.kitchen.url:http://kitchen-svc:8081}")
public interface KitchenClient {
    @PostMapping("/api/kitchen/orders")
    KitchenOrderResponse createKitchenOrder(@RequestBody KitchenOrderRequest request);

    @GetMapping("/api/kitchen/orders/{id}")
    KitchenOrderResponse getKitchenOrder(@PathVariable("id") UUID id);

    @PutMapping("/api/kitchen/orders/{id}/status")
    KitchenOrderResponse updateKitchenOrderStatus(@PathVariable("id") UUID id, @RequestBody KitchenOrderStatusUpdate request);

    /**
     * Modified to match kitchen service CreateKitchenOrderRequest:
     * { "orderId": UUID, "itemsJson": "JSON string" }
     */
    class KitchenOrderRequest {
        @JsonProperty("orderId")
        public UUID orderId;

        @JsonProperty("itemsJson")
        public String itemsJson;
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