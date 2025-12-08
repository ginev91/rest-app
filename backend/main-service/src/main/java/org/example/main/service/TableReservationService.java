package org.example.main.service;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.TableReservationEntity;
import org.example.main.model.enums.ReservationStatus;
import org.example.main.repository.TableReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TableReservationService implements ITableReservationService {

    private final TableReservationRepository reservationRepository;

    public TableReservationService(TableReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    public TableReservationEntity reserveTable(UUID tableId, OffsetDateTime from, OffsetDateTime to, UUID requestedBy, UUID userId) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reservation window");
        }
        List<TableReservationEntity> conflicts = reservationRepository.findByTableIdAndEndTimeAfterAndStartTimeBefore(tableId, from, to);
        if (!conflicts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Table already reserved for this time");
        }
        TableReservationEntity r = new TableReservationEntity();
        r.setTableId(tableId);
        r.setUserId(userId);
        r.setStartTime(from);
        r.setEndTime(to);
        r.setCreatedBy(requestedBy);
        r.setStatus(ReservationStatus.ACTIVE);
        return reservationRepository.save(r);
    }

    @Override
    public TableReservationEntity cancelReservation(UUID reservationId, UUID cancelledBy) {
        TableReservationEntity r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));
        r.setStatus(ReservationStatus.CANCELLED);
        r.setDeleted(true);
        r.setUpdatedAt(OffsetDateTime.now());
        return reservationRepository.save(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableReservationEntity> findActiveReservationsForTable(UUID tableId) {
        return reservationRepository.findByTableIdAndDeletedFalse(tableId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableReservationEntity> findReservationHistoryForTable(UUID tableId) {
        
        return reservationRepository.findByTableIdAndDeletedFalse(tableId); 
    }
}