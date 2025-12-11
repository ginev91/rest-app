package org.example.main.service.table;

import org.example.main.model.table.TableReservationEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ITableReservationService {
    TableReservationEntity reserveTable(UUID tableId, OffsetDateTime from, OffsetDateTime to, UUID requestedBy, UUID userId);
    TableReservationEntity cancelReservation(UUID reservationId, UUID cancelledBy);
    List<TableReservationEntity> findActiveReservationsForTable(UUID tableId);
    List<TableReservationEntity> findReservationHistoryForTable(UUID tableId);
}