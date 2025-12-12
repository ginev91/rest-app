package org.example.main.dto.response.user;

import lombok.*;

import java.util.UUID;

/**
 * Lombok-backed DTO with builder to be used by UserMapper.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserProfileResponseDto {
    private UUID id;
    private String username;
    private String fullName;
    private String role;
    private Boolean blocked;
}