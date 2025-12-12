package org.example.main.dto.request.user;

import lombok.*;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockUserRequestDto {
    @NotNull
    private Boolean blocked;
}