package org.example.main.repository;

import org.example.main.model.TableReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface TableReservationRepository extends JpaRepository<TableReservation, UUID> {
    List<TableReservation> findByTableIdAndEndTimeAfterAndStartTimeBefore(UUID tableId, OffsetDateTime start, OffsetDateTime end);
    List<TableReservation> findByTableId(UUID tableId);
}