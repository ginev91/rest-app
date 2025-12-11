package org.example.main.dto.request.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateProfileRequestDto {

    @Size(max = 200, message = "fullName must be at most 200 characters")
    private String fullName;

    @Size(max = 50, message = "username must be at most 50 characters")
    private String username;
}