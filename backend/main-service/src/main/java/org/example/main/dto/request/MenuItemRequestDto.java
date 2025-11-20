package org.example.main.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO used for creating/updating MenuItem via REST API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemRequestDto {

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must be at most 255 characters")
    private String name;

    @Size(max = 1000, message = "description must be at most 1000 characters")
    private String description;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "price must be non-negative")
    private BigDecimal price;
}