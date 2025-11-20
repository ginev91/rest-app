package org.example.kitchen.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateKitchenOrderRequest {
    @NotNull
    private UUID orderId;

    // JSON or simple string describing items
    @NotBlank
    private String itemsJson;
}