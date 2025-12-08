package org.example.main.dto.response;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponseDto {
    private UUID id;
    private String name;
}