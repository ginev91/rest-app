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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class RestaurantTableService implements IRestaurantTableService {

    private static final Pattern CODE_TABLE_NUMBER = Pattern.compile("^T(\\d+)$", Pattern.CASE_INSENSITIVE);

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
        if (table == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table payload is required");

        // Normalize code to uppercase if present
        if (table.getCode() != null) {
            table.setCode(table.getCode().trim().toUpperCase());
        }

        // If code encodes a table number (e.g. T12) set the tableNumber accordingly
        if (table.getCode() != null) {
            Matcher m = CODE_TABLE_NUMBER.matcher(table.getCode());
            if (m.matches()) {
                try {
                    int parsedNumber = Integer.parseInt(m.group(1));
                    table.setTableNumber(parsedNumber);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // If tableNumber present, ensure uniqueness (no other table has same number)
        if (table.getTableNumber() != null) {
            Optional<RestaurantTable> byNumber = tableRepository.findByTableNumber(table.getTableNumber());
            if (byNumber.isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Table number already exists: " + table.getTableNumber());
            }
        }

        // If code present, ensure uniqueness
        if (table.getCode() != null) {
            Optional<RestaurantTable> byCode = tableRepository.findByCode(table.getCode());
            if (byCode.isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Table code already exists: " + table.getCode());
            }
        }

        // Pin handling (string of digits, max length 4)
        if (table.getPinCode() == null || table.getPinCode().trim().isEmpty()) {
            table.setPinCode(RestaurantTableUtils.generatePinCode());
        } else {
            String sanitized = RestaurantTableUtils.sanitizePin(table.getPinCode());
            if (!RestaurantTableUtils.isValidPin(sanitized)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pinCode must be 1..4 digits");
            }
            table.setPinCode(sanitized);
        }

        // Default seats if null
        if (table.getSeats() == null || table.getSeats() == 0) {
            table.setSeats(4);
        }

        // Defaults for occupancy/status
        if (table.getCurrentOccupancy() == null) table.setCurrentOccupancy(0);
        if (table.getStatus() == null) table.setStatus(TableStatus.AVAILABLE);

        try {
            return tableRepository.save(table);
        } catch (DataIntegrityViolationException dive) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot create table due to constraint violation");
        }
    }

    @Override
    public RestaurantTable update(UUID id, RestaurantTable changes) {
        RestaurantTable existing = findById(id);

        // If code is being changed, normalize and possibly derive tableNumber
        if (changes.getCode() != null) {
            String newCode = changes.getCode().trim().toUpperCase();
            // check for duplicate code (exclude current)
            Optional<RestaurantTable> byCode = tableRepository.findByCode(newCode);
            if (byCode.isPresent() && !byCode.get().getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Table code already exists: " + newCode);
            }
            existing.setCode(newCode);

            // If new code encodes a table number, use it (and check uniqueness)
            Matcher m = CODE_TABLE_NUMBER.matcher(newCode);
            if (m.matches()) {
                try {
                    int parsedNumber = Integer.parseInt(m.group(1));
                    if (existing.getTableNumber() == null || existing.getTableNumber() != parsedNumber) {
                        Optional<RestaurantTable> byNumber = tableRepository.findByTableNumber(parsedNumber);
                        if (byNumber.isPresent() && !byNumber.get().getId().equals(id)) {
                            throw new ResponseStatusException(HttpStatus.CONFLICT, "Table number already exists: " + parsedNumber);
                        }
                        existing.setTableNumber(parsedNumber);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (changes.getSeats() != null && changes.getSeats() != 0) existing.setSeats(changes.getSeats());

        // If tableNumber explicitly provided in payload, validate uniqueness and set it
        if (changes.getTableNumber() != null) {
            Integer newNumber = changes.getTableNumber();
            if (!newNumber.equals(existing.getTableNumber())) {
                Optional<RestaurantTable> byNumber = tableRepository.findByTableNumber(newNumber);
                if (byNumber.isPresent() && !byNumber.get().getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Table number already exists: " + newNumber);
                }
                existing.setTableNumber(newNumber);
            }
        }

        // If pin provided update it, else if null leave existing; validate format
        if (changes.getPinCode() != null) {
            String sanitized = RestaurantTableUtils.sanitizePin(changes.getPinCode());
            if (!RestaurantTableUtils.isValidPin(sanitized)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pinCode must be 1..4 digits");
            }
            existing.setPinCode(sanitized);
        } else if (existing.getPinCode() == null || existing.getPinCode().trim().isEmpty()) {
            // ensure numeric pin exists
            existing.setPinCode(RestaurantTableUtils.generatePinCode());
        }

        try {
            return tableRepository.save(existing);
        } catch (DataIntegrityViolationException dive) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot update table due to constraint violation");
        }
    }

    @Override
    public void delete(UUID id) {
        // Ensure the table exists and perform safety checks before deleting
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable not found: " + id));

        OffsetDateTime now = OffsetDateTime.now();

        // Prevent deleting if table is currently occupied (occupiedUntil in future or status OCCUPIED with null occupiedUntil)
        if (table.getStatus() == TableStatus.OCCUPIED) {
            OffsetDateTime occUntil = table.getOccupiedUntil();
            if (occUntil == null || occUntil.isAfter(now)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete table while it is occupied");
            }
        }

        // Prevent deleting if there are future reservations for this table
        List<TableReservationEntity> reservations = reservationRepository.findByTableId(id);
        boolean hasFutureReservations = reservations.stream()
                .anyMatch(r -> r.getEndTime() != null && r.getEndTime().isAfter(now));
        if (hasFutureReservations) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete table with future reservations");
        }

        // Final attempt to delete — catch DB integrity violations as a defensive fallback
        try {
            tableRepository.deleteById(id);
        } catch (DataIntegrityViolationException dive) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete table with existing relations");
        }
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
                t.setPinCode(RestaurantTableUtils.generatePinCode());
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