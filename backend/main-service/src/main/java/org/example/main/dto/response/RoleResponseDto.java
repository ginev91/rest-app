package org.example.main.dto.response;

import lombok.*;

import java.util.UUID;

/**
 * Simple response DTO for Role returned by controllers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponseDto {
    private UUID id;
    private String name;
}