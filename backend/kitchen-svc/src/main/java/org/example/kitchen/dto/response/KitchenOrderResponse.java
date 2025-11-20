package org.example.kitchen.dto.response;

import lombok.Data;
import org.example.kitchen.model.KitchenOrderStatus;

import java.time.Instant;
import java.util.UUID;

@Data
public class KitchenOrderResponse {
    private UUID id;
    private UUID orderId;
    private String itemsJson;
    private KitchenOrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}