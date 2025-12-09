package org.example.main.dto.response;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class AuthResponseDto {
    private String token;
    private String username;
    private UUID userId;
    private String role;
    private Integer tableNumber;
    private UUID tableId;

    public AuthResponseDto(String token, String username, UUID id, String role) {
        this.token = token;
        this.username = username;
        this.userId = id;
        this.role = role;
    }
}