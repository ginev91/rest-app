package org.example.main.dto.response.menu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.main.model.enums.Macros;
import org.example.main.model.enums.ItemType;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemResponseDto {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private int calories;
    private boolean available;
    private ItemType itemType;
    private UUID categoryId;
    private Macros macros;
}