package org.example.main.service.table;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.table.TableReservationEntity;
import org.example.main.model.enums.ReservationStatus;
import org.example.main.repository.table.TableReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Override
    public List<TableReservationEntity> findReservationsByDate(LocalDate date) {
        if (date == null) {
            return List.of();
        }

        ZoneId zone = ZoneOffset.UTC;
        OffsetDateTime dayStart = date.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime dayEnd = date.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        return reservationRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(r -> {
                    OffsetDateTime from = r.getStartTime();
                    OffsetDateTime to = r.getEndTime();
                    if (from == null) return false;
                    if (to == null) {
                        return from.isBefore(dayEnd) && (from.isAfter(dayStart) || from.isEqual(dayStart));
                    }
                    return from.isBefore(dayEnd) && to.isAfter(dayStart);
                })
                .sorted(Comparator.comparing(TableReservationEntity::getStartTime))
                .collect(Collectors.toList());
    }
}