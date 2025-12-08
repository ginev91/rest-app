package org.example.main.controller;

import org.example.main.dto.request.ReservationRequestDto;
import org.example.main.model.RestaurantTable;
import org.example.main.model.TableReservationEntity;
import org.example.main.service.RestaurantTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.example.main.dto.request.OccupyRequest;

import java.util.List;
import java.util.UUID;

/**
 * Controller exposing reservation and occupancy endpoints.
 *
 * - POST /api/tables/occupy
 *   Body: { "tableNumber": 5, "minutes": 60 }
 *   This is used by backend OrderService after order creation; frontend does not need to call it.
 */
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

    @PostMapping("/{tableId}/reserve")
    public ResponseEntity<TableReservationEntity> reserve(
            @PathVariable UUID tableId,
            @RequestBody ReservationRequestDto req,
            @RequestHeader(value = "X-User-Id", required = false) UUID requesterId) {

        // TODO: Add @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')") if security is configured.
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
        // Used by backend order flow; kept open for convenience (no auth here; secure in prod)
        service.occupyTable(req.tableNumber, req.minutes);
        return ResponseEntity.ok().build();
    }
}