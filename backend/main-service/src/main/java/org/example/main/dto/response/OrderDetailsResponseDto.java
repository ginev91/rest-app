package org.example.main.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailsResponseDto {
    private String id;
    private String userId;
    private String userName;
    private Integer tableNumber;       // nullable if not used
    private String status;             // e.g. "preparing", "completed"
    private BigDecimal totalAmount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<OrderItemResponseDto> items;
}