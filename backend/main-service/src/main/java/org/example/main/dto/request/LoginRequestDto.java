package org.example.main.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class LoginRequestDto {
    @NotBlank(message = "username must not be blank")
    private String username;

    @NotBlank(message = "password must not be blank")
    private String password;

    @Positive(message = "tableNumber must be positive")
    private Integer tableNumber;


    @Size(min = 4, max = 6, message = "pinCode must be between 4 and 6 characters")
    @Pattern(regexp = "\\d+", message = "pinCode must be numeric")
    private String tablePin;
}