package org.example.main.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailsResponseDto {

    private String id;
    private String userId;
    private String userName;
    private Integer tableNumber;
    private String status;
    private BigDecimal totalAmount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private String kitchenOrderId;
    private String kitchenStatus;

    private List<OrderItemResponseDto> items;
}