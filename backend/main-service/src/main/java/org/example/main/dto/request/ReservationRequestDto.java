package org.example.main.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequestDto {
    @NotNull
    private UUID tableId;

    @NotNull
    private OffsetDateTime from;

    @NotNull
    private OffsetDateTime to;

    @NotNull
    private UUID requestedBy;

    @NotNull
    private UUID userId;
}