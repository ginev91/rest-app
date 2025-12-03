package org.example.main.service;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.RestaurantTable;
import org.example.main.model.TableReservation;
import org.example.main.repository.RestaurantTableRepository;
import org.example.main.repository.TableReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RestaurantTableService: keeps previous behaviour and adds:
 *  - occupyTable(tableNumber, minutes) -> mark table occupied until now + minutes
 *  - reserveTable(tableId, from, to, requestedBy) -> create reservation if no overlap
 *
 * Note: We add tableNumber and occupiedUntil fields to RestaurantTable (DB migration via ddl-auto=update).
 *
 * Keep existing method names used by other code.
 */
@Service
@Transactional
public class RestaurantTableService implements IRestaurantTableService {

    private final RestaurantTableRepository tableRepository;
    private final TableReservationRepository reservationRepository;

    public RestaurantTableService(RestaurantTableRepository tableRepository,
                                  TableReservationRepository reservationRepository) {
        this.tableRepository = tableRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantTable> findAll() {
        return tableRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantTable findById(UUID id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable not found: " + id));
    }

    @Override
    public RestaurantTable create(RestaurantTable table) {
        return tableRepository.save(table);
    }

    @Override
    public RestaurantTable update(UUID id, RestaurantTable changes) {
        RestaurantTable existing = findById(id);
        if (changes.getCode() != null) existing.setCode(changes.getCode());
        if (changes.getSeats() != null && changes.getSeats() != 0) existing.setSeats(changes.getSeats());
        if (changes.getTableNumber() != null) existing.setTableNumber(changes.getTableNumber());
        return tableRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        if (!tableRepository.existsById(id)) {
            throw new ResourceNotFoundException("RestaurantTable not found: " + id);
        }
        tableRepository.deleteById(id);
    }

    /**
     * Mark table with given tableNumber as occupied for a given number of minutes.
     * If no RestaurantTable exists for the number, creates one (with default values).
     */
    @Transactional
    public void occupyTable(Integer tableNumber, int forMinutes) {
        if (tableNumber == null) return;
        RestaurantTable table = tableRepository.findByTableNumber(tableNumber)
                .orElseGet(() -> {
                    RestaurantTable t = new RestaurantTable();
                    t.setCode("T" + tableNumber);
                    t.setSeats(4);
                    t.setPinCode(UUID.randomUUID().toString().substring(0, 6));
                    t.setTableNumber(tableNumber);
                    return t;
                });

        table.setOccupiedUntil(OffsetDateTime.now().plusMinutes(forMinutes));
        table.setStatus(org.example.main.model.enums.TableStatus.OCCUPIED);
        tableRepository.save(table);
    }

    /**
     * Reserve a table for a time window. Rejects overlapping reservations.
     * Returns the saved TableReservation.
     */
    @Transactional
    public TableReservation reserveTable(UUID tableId, OffsetDateTime from, OffsetDateTime to, UUID requestedBy, UUID userId) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reservation window");
        }
        // check conflicts
        List<TableReservation> conflicts = reservationRepository.findByTableIdAndEndTimeAfterAndStartTimeBefore(tableId, from, to);
        if (!conflicts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Table already reserved for this time");
        }
        TableReservation r = TableReservation.builder()
                .tableId(tableId)
                .userId(userId)
                .startTime(from)
                .endTime(to)
                .createdBy(requestedBy)
                .build();
        return reservationRepository.save(r);
    }

    public Optional<RestaurantTable> findByTableNumber(Integer tableNumber) {
        return tableRepository.findByTableNumber(tableNumber);
    }

    public List<TableReservation> findReservationsForTable(UUID tableId) {
        return reservationRepository.findByTableId(tableId);
    }
}