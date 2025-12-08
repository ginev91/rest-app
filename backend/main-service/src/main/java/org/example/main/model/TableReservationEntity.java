package org.example.main.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.main.model.enums.ReservationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "table_reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableReservationEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "table_id", columnDefinition = "uuid", nullable = false)
    private UUID tableId;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "created_by", columnDefinition = "uuid", nullable = false)
    private UUID createdBy;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}