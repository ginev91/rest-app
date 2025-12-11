package org.example.main.service.table;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.table.RestaurantTable;
import org.example.main.model.table.TableReservationEntity;
import org.example.main.model.enums.TableStatus;
import org.example.main.repository.table.RestaurantTableRepository;
import org.example.main.repository.table.TableReservationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Transactional
    public void occupyTable(Integer tableNumber, int forMinutes) {
        if (tableNumber == null) return;

        String code = "T" + tableNumber;
        RestaurantTable table = null;

        
        Optional<RestaurantTable> byNumber = tableRepository.findByTableNumber(tableNumber);
        if (byNumber.isPresent()) {
            table = byNumber.get();
        } else {
            
            Optional<RestaurantTable> byCode = tableRepository.findByCode(code);
            if (byCode.isPresent()) {
                table = byCode.get();
                if (table.getTableNumber() == null) {
                    table.setTableNumber(tableNumber);
                }
            } else {
                
                RestaurantTable t = new RestaurantTable();
                t.setCode(code);
                t.setSeats(4);
                t.setPinCode(UUID.randomUUID().toString().substring(0, 6));
                t.setTableNumber(tableNumber);
                table = t;
            }
        }

        table.setOccupiedUntil(OffsetDateTime.now().plusMinutes(forMinutes));
        table.setStatus(TableStatus.OCCUPIED);

        try {
            tableRepository.save(table);
        } catch (DataIntegrityViolationException dive) {
            
            Optional<RestaurantTable> existing = tableRepository.findByCode(code);
            if (existing.isPresent()) {
                RestaurantTable e = existing.get();
                if (e.getTableNumber() == null) e.setTableNumber(tableNumber);
                e.setOccupiedUntil(table.getOccupiedUntil());
                e.setStatus(TableStatus.OCCUPIED);
                tableRepository.save(e);
            } else {
                
                throw dive;
            }
        }
    }

    @Transactional
    public TableReservationEntity reserveTable(UUID tableId, OffsetDateTime from, OffsetDateTime to, UUID requestedBy, UUID userId) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reservation window");
        }
        List<TableReservationEntity> conflicts = reservationRepository.findByTableIdAndEndTimeAfterAndStartTimeBefore(tableId, from, to);
        if (!conflicts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Table already reserved for this time");
        }
        TableReservationEntity r = TableReservationEntity.builder()
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

    public List<TableReservationEntity> findReservationsForTable(UUID tableId) {
        return reservationRepository.findByTableId(tableId);
    }
}