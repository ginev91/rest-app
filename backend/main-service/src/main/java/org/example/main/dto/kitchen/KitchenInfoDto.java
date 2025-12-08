package org.example.main.dto.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lightweight DTO used in the Main app to represent kitchen order info returned by kitchen-service.
 * This is NOT a JPA entity and will NOT create a DB table.
 */
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