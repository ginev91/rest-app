package org.example.main.dto.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDto {
    private UUID orderId;
    private String status;
    private BigDecimal totalAmount;
    private Integer tableNumber;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<OrderItemResponseDto> items;
    private String username;
    private String waiterId;
}