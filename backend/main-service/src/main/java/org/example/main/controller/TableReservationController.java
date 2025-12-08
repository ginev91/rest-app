package org.example.main.controller;

import org.example.main.dto.request.CancelReservationRequestDto;
import org.example.main.dto.request.ReservationRequestDto;
import org.example.main.dto.response.TableReservationResponseDto;
import org.example.main.mapper.ReservationMapper;
import org.example.main.model.TableReservationEntity;
import org.example.main.service.ITableReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservations")
public class TableReservationController {

    private final ITableReservationService reservationService;
    private final ReservationMapper mapper;

    public TableReservationController(ITableReservationService reservationService, ReservationMapper mapper) {
        this.reservationService = reservationService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<TableReservationResponseDto> createReservation(@Valid @RequestBody ReservationRequestDto req) {
        TableReservationEntity created = reservationService.reserveTable(
                req.getTableId(), req.getFrom(), req.getTo(), req.getRequestedBy(), req.getUserId()
        );
        return ResponseEntity.ok(mapper.toResponse(created));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TableReservationResponseDto> cancel(@PathVariable("id") UUID id,
                                                              @Valid @RequestBody CancelReservationRequestDto body) {
        TableReservationEntity cancelled = reservationService.cancelReservation(id, body.getCancelledBy());
        return ResponseEntity.ok(mapper.toResponse(cancelled));
    }

    @GetMapping("/table/{tableId}")
    public ResponseEntity<List<TableReservationResponseDto>> getActiveForTable(@PathVariable("tableId") UUID tableId) {
        List<TableReservationResponseDto> dtos = reservationService.findActiveReservationsForTable(tableId)
                .stream().map(mapper::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/table/{tableId}/history")
    public ResponseEntity<List<TableReservationResponseDto>> getHistoryForTable(@PathVariable("tableId") UUID tableId) {
        List<TableReservationResponseDto> dtos = reservationService.findReservationHistoryForTable(tableId)
                .stream().map(mapper::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}