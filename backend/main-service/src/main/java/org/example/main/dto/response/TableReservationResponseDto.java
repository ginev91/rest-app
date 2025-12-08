package org.example.main.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableReservationResponseDto {
    private UUID id;
    private UUID tableId;
    private UUID userId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String status; // use enum.name()
    private boolean deleted;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}