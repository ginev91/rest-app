package org.example.main.dto.request.category;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.example.main.model.enums.ItemType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class CategoryRequestDto {
    @NotNull
    private ItemType itemType;
}