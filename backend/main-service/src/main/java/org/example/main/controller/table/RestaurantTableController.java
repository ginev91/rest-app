package org.example.main.controller.table;

import org.example.main.dto.request.table.ReservationRequestDto;
import org.example.main.dto.request.table.OccupyRequest;
import org.example.main.model.table.RestaurantTable;
import org.example.main.model.table.TableReservationEntity;
import org.example.main.service.table.RestaurantTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tables")
public class RestaurantTableController {

    private static final Logger log = LoggerFactory.getLogger(RestaurantTableController.class);
    private final RestaurantTableService service;

    public RestaurantTableController(RestaurantTableService service) {
        this.service = service;
    }

    @GetMapping()
    public ResponseEntity<List<RestaurantTable>> getTables() {
        List<RestaurantTable> list = service.findAll();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{tableId}")
    public ResponseEntity<RestaurantTable> getTable(@PathVariable UUID tableId) {
        RestaurantTable t = service.findById(tableId);
        return ResponseEntity.ok(t);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestaurantTable> createTable(@RequestBody RestaurantTable table) {
        RestaurantTable saved = service.create(table);
        return ResponseEntity.created(URI.create("/api/tables/" + saved.getId())).body(saved);
    }

    // Update table metadata (ADMIN only)
    @PutMapping("/{tableId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestaurantTable> updateTable(@PathVariable UUID tableId, @RequestBody RestaurantTable changes) {
        RestaurantTable updated = service.update(tableId, changes);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{tableId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTable(@PathVariable UUID tableId) {
        service.delete(tableId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tableId}/reserve")
    public ResponseEntity<TableReservationEntity> reserve(
            @PathVariable UUID tableId,
            @RequestBody ReservationRequestDto req,
            @RequestHeader(value = "X-User-Id", required = false) UUID requesterId) {

        TableReservationEntity saved = service.reserveTable(tableId, req.getFrom(), req.getTo(), requesterId, req.getUserId());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{tableId}/reservations")
    public ResponseEntity<List<TableReservationEntity>> getReservations(@PathVariable UUID tableId) {
        List<TableReservationEntity> list = service.findReservationsForTable(tableId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/occupy")
    public ResponseEntity<Void> occupy(@RequestBody OccupyRequest req) {
        service.occupyTable(req.tableNumber, req.minutes);
        return ResponseEntity.ok().build();
    }
}