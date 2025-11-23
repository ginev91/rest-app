package org.example.main.dto.response;

import lombok.*;

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
}