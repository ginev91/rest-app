package org.example.main.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * DTO used by POST /api/orders
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDto {

    @NotNull
    private UUID tableId;

    private Integer tableNumber;

    private UUID customerId;

    private String customerName;

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemRequest {
        @NotNull
        private UUID menuItemId;

        @NotNull
        @Min(1)
        private Integer quantity;
    }
}