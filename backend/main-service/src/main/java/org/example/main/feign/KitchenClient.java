package org.example.main.feign;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for kitchen-service.
 *
 * This version is backward-compatible with your previous DTO shapes (keeps `id`, `sourceOrderId`,
 * `status`, `note` fields) and extends the client with:
 *  - cancel endpoint (POST /api/kitchen/orders/{id}/cancel)
 *  - get-by-order endpoint (GET /api/kitchen/orders/by-order/{orderId})
 *
 * Configure URL with property: feign.kitchen.url (default points to http://kitchen-svc:8081)
 */
@FeignClient(name = "kitchen-service", url = "${feign.kitchen.url:http://kitchen-svc:8081}")
public interface KitchenClient {

    @PostMapping("/api/kitchen/orders")
    KitchenOrderResponse createKitchenOrder(@RequestBody KitchenOrderRequest request);

    @GetMapping("/api/kitchen/orders/{id}")
    KitchenOrderResponse getKitchenOrder(@PathVariable("id") UUID id);

    @PutMapping("/api/kitchen/orders/{id}/status")
    KitchenOrderResponse updateKitchenOrderStatus(@PathVariable("id") UUID id, @RequestBody KitchenOrderStatusUpdate request);

    /**
     * Cancel endpoint — kitchen-service enforces domain rules (irreversible if not allowed).
     * Uses POST to align with kitchen controller design for a dedicated cancel action.
     */
    @PostMapping("/api/kitchen/orders/{id}/cancel")
    void cancelKitchenOrder(@PathVariable("id") UUID id);

    /**
     * Get kitchen orders by source order id (one source order may map to multiple kitchen orders).
     * Useful for order detail page to show kitchen progress.
     */
    @GetMapping("/api/kitchen/orders/by-order/{orderId}")
    List<KitchenOrderResponse> getByOrder(@PathVariable("orderId") UUID orderId);

    /**
     * --- DTOs ---
     *
     * Note: KitchenOrderResponse preserves the original field names you used previously (id, sourceOrderId, status, note)
     * and also accepts common kitchen-service payload keys (orderId, itemsJson, createdAt, updatedAt) via
     * JsonProperty annotations so responses from kitchen-service will deserialize correctly.
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