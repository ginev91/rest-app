package org.example.main.dto.response;

import lombok.*;
import org.example.main.model.enums.OrderItemStatus;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponseDto {
    private String menuItemId;
    private String menuItemName;
    private int quantity;
    private BigDecimal price;
    private OrderItemStatus status;
}