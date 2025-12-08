package org.example.main.feign;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "kitchen-service", url = "${feign.kitchen.url:http://kitchen-svc:8081}")
public interface KitchenClient {

    @PostMapping("/api/kitchen/orders")
    KitchenOrderResponse createKitchenOrder(@RequestBody KitchenOrderRequest request);

    @GetMapping("/api/kitchen/orders/{id}")
    KitchenOrderResponse getKitchenOrder(@PathVariable("id") UUID id);

    @PutMapping("/api/kitchen/orders/{id}/status")
    KitchenOrderResponse updateKitchenOrderStatus(@PathVariable("id") UUID id, @RequestBody KitchenOrderStatusUpdate request);

    @PostMapping("/api/kitchen/orders/{id}/cancel")
    void cancelKitchenOrder(@PathVariable("id") UUID id);

    @GetMapping("/api/kitchen/orders/by-order/{orderId}")
    List<KitchenOrderResponse> getByOrder(@PathVariable("orderId") UUID orderId);

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

        @JsonProperty("orderId")
        public UUID sourceOrderId;

        @JsonProperty("sourceOrderId")
        public UUID sourceOrderIdAlias;

        @JsonProperty("itemsJson")
        public String itemsJson;

        public String status;

        public String note;

        @JsonProperty("createdAt")
        public String createdAt;

        @JsonProperty("updatedAt")
        public String updatedAt;
    }

    class KitchenOrderStatusUpdate {
        public String status;

        public KitchenOrderStatusUpdate() {}
        public KitchenOrderStatusUpdate(String status) { this.status = status; }
    }
}