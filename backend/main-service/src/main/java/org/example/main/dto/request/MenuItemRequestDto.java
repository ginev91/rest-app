package org.example.main.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.example.main.model.Macros;
import org.example.main.model.enums.ItemType;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class MenuItemRequestDto {

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must be at most 255 characters")
    private String name;

    @Size(max = 1000, message = "description must be at most 1000 characters")
    private String description;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "price must be non-negative")
    private BigDecimal price;

    @Min(value = 0, message = "calories must be non-negative")
    private int calories;

    private boolean available;

    @NotNull(message = "itemType is required")
    private ItemType itemType;

    private UUID categoryId;

    private Macros macros;
}