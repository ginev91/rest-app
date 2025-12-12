package org.example.main.dto.request.table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantTableRequestDto {
    private String code;

    @Min(value = 1, message = "seats must be >= 1")
    private Integer seats;

    private Integer tableNumber;

    private String pinCode;
}