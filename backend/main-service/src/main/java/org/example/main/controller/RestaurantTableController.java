package org.example.main.controller;

import org.example.main.model.TableReservation;
import org.example.main.service.RestaurantTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.example.main.dto.request.OccupyRequest;
import org.example.main.dto.request.ReserveRequest;

import java.util.List;
import java.util.UUID;

/**
 * Controller exposing reservation and occupancy endpoints.
 *
 * Notes for frontend:
 * - POST /api/tables/{tableId}/reserve
 *   Body: { "from":"2025-12-03T12:00:00+02:00", "to":"2025-12-03T13:00:00+02:00", "userId":"<uuid>" }
 *   Requires ROLE_ADMIN or ROLE_EMPLOYEE (configure @PreAuthorize as needed).
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

    @PostMapping("/{tableId}/reserve")
    public ResponseEntity<TableReservation> reserve(
            @PathVariable UUID tableId,
            @RequestBody ReserveRequest req,
            @RequestHeader(value = "X-User-Id", required = false) UUID requesterId) {

        // TODO: Add @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')") if security is configured.
        TableReservation saved = service.reserveTable(tableId, req.from, req.to, requesterId, req.userId);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{tableId}/reservations")
    public ResponseEntity<List<TableReservation>> getReservations(@PathVariable UUID tableId) {
        List<TableReservation> list = service.findReservationsForTable(tableId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/occupy")
    public ResponseEntity<Void> occupy(@RequestBody OccupyRequest req) {
        // Used by backend order flow; kept open for convenience (no auth here; secure in prod)
        service.occupyTable(req.tableNumber, req.minutes);
        return ResponseEntity.ok().build();
    }
}