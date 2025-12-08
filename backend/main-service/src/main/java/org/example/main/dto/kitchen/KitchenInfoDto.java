package org.example.main.dto.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KitchenInfoDto {
    private UUID kitchenOrderId;
    private UUID sourceOrderId;
    private String status;
    private String note;
    private String itemsJson;
    private String createdAt;
    private String updatedAt;
}