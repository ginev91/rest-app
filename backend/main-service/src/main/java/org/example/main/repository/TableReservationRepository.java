package org.example.main.repository;

import org.example.main.model.TableReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface TableReservationRepository extends JpaRepository<TableReservationEntity, UUID> {
    List<TableReservationEntity> findByTableId(UUID tableId);
    List<TableReservationEntity> findByTableIdAndEndTimeAfterAndStartTimeBefore(UUID tableId, OffsetDateTime endTime, OffsetDateTime startTime);
    List<TableReservationEntity> findByTableIdAndDeletedFalse(UUID tableId);
}