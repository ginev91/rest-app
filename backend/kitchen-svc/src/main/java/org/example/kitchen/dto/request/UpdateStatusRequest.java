package org.example.kitchen.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import org.example.kitchen.model.enums.KitchenOrderStatus;

@Data
public class UpdateStatusRequest {
    @NotNull
    private KitchenOrderStatus status;
}