package org.example.main.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelReservationRequestDto {
    @NotNull
    private UUID cancelledBy;
}