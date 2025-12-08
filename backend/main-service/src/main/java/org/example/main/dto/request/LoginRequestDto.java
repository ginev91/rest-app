package org.example.main.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login request DTO.
 * - username and password are mandatory.
 * - tableNumber/tablePin are optional here and validated in UserService.login for ROLE_USER.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDto {

    @NotBlank(message = "username must not be blank")
    private String username;

    @NotBlank(message = "password must not be blank")
    private String password;

    /**
     * Optional table number for clients (ROLE_USER). Service enforces presence when required.
     */
    @Positive(message = "tableNumber must be positive")
    private Integer tableNumber;

    /**
     * Optional table PIN for clients (ROLE_USER). Service enforces presence when required.
     */
    @Size(min = 4, max = 6, message = "tablePin must be between 4 and 6 characters")
    @Pattern(regexp = "\\d+", message = "tablePin must be numeric")
    private String tablePin;
}