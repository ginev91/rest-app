package org.example.main.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDto {
    @NotNull(message = "tableId is required")
    private UUID tableId;

    @NotNull(message = "customerId is required")
    private UUID customerId;

    @NotEmpty(message = "items must not be empty")
    @Size(min = 1, message = "there must be at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemRequest {
        @NotNull(message = "menuItemId is required")
        private UUID menuItemId;

        @NotNull(message = "quantity is required")
        @jakarta.validation.constraints.Positive(message = "quantity must be positive")
        private Integer quantity;
    }
}