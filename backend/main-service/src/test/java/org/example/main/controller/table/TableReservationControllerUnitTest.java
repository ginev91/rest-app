package org.example.main.controller.table;

import org.example.main.dto.request.table.CancelReservationRequestDto;
import org.example.main.dto.request.table.ReservationRequestDto;
import org.example.main.dto.response.table.TableReservationResponseDto;
import org.example.main.mapper.table.ReservationMapper;
import org.example.main.model.table.TableReservationEntity;
import org.example.main.service.table.ITableReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TableReservationControllerTest {

    @Mock
    ITableReservationService reservationService;

    @Mock
    ReservationMapper mapper;

    @InjectMocks
    TableReservationController controller;

    @Test
    void createReservation_returnsMappedDto_onSuccess() {
        UUID tableId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requestedBy = UUID.randomUUID();

        ReservationRequestDto req = new ReservationRequestDto();
        req.setTableId(tableId);
        req.setFrom(OffsetDateTime.now().plusHours(1));
        req.setTo(req.getFrom().plusHours(2));
        req.setRequestedBy(requestedBy);
        req.setUserId(userId);

        TableReservationEntity createdEntity = TableReservationEntity.builder()
                .id(UUID.randomUUID())
                .tableId(tableId)
                .startTime(req.getFrom())
                .endTime(req.getTo())
                .userId(userId)
                .createdBy(requestedBy)
                .build();

        TableReservationResponseDto dto = mock(TableReservationResponseDto.class);

        when(reservationService.reserveTable(tableId, req.getFrom(), req.getTo(), requestedBy, userId))
                .thenReturn(createdEntity);
        when(mapper.toResponse(createdEntity)).thenReturn(dto);

        ResponseEntity<TableReservationResponseDto> resp = controller.createReservation(req);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(dto);

        verify(reservationService).reserveTable(tableId, req.getFrom(), req.getTo(), requestedBy, userId);
        verify(mapper).toResponse(createdEntity);
    }

    @Test
    void createReservation_propagatesServiceError() {
        ReservationRequestDto req = new ReservationRequestDto();
        req.setTableId(UUID.randomUUID());
        req.setFrom(OffsetDateTime.now().plusHours(2));
        req.setTo(req.getFrom().minusHours(1));

        when(reservationService.reserveTable(any(), any(), any(), any(), any()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "invalid"));

        assertThatThrownBy(() -> controller.createReservation(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalid");

        verify(reservationService).reserveTable(any(), any(), any(), any(), any());
        verifyNoInteractions(mapper);
    }

    @Test
    void cancel_returnsMappedDto_onSuccess() {
        UUID id = UUID.randomUUID();
        UUID cancelledBy = UUID.randomUUID();

        CancelReservationRequestDto body = new CancelReservationRequestDto();
        body.setCancelledBy(cancelledBy);

        TableReservationEntity cancelled = TableReservationEntity.builder()
                .id(id)
                .tableId(UUID.randomUUID())
                .createdBy(cancelledBy)
                .build();

        TableReservationResponseDto dto = mock(TableReservationResponseDto.class);

        when(reservationService.cancelReservation(id, cancelledBy)).thenReturn(cancelled);
        when(mapper.toResponse(cancelled)).thenReturn(dto);

        ResponseEntity<TableReservationResponseDto> resp = controller.cancel(id, body);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(dto);

        verify(reservationService).cancelReservation(id, cancelledBy);
        verify(mapper).toResponse(cancelled);
    }

    @Test
    void cancel_propagatesServiceError() {
        UUID id = UUID.randomUUID();
        CancelReservationRequestDto body = new CancelReservationRequestDto();
        body.setCancelledBy(UUID.randomUUID());

        when(reservationService.cancelReservation(eq(id), any()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "not found"));

        assertThatThrownBy(() -> controller.cancel(id, body))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");

        verify(reservationService).cancelReservation(eq(id), any());
        verifyNoInteractions(mapper);
    }

    @Test
    void getActiveForTable_returnsMappedList() {
        UUID tableId = UUID.randomUUID();

        TableReservationEntity e1 = TableReservationEntity.builder().id(UUID.randomUUID()).tableId(tableId).build();
        TableReservationEntity e2 = TableReservationEntity.builder().id(UUID.randomUUID()).tableId(tableId).build();

        TableReservationResponseDto d1 = mock(TableReservationResponseDto.class);
        TableReservationResponseDto d2 = mock(TableReservationResponseDto.class);

        when(reservationService.findActiveReservationsForTable(tableId)).thenReturn(List.of(e1, e2));
        when(mapper.toResponse(e1)).thenReturn(d1);
        when(mapper.toResponse(e2)).thenReturn(d2);

        ResponseEntity<List<TableReservationResponseDto>> resp = controller.getActiveForTable(tableId);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).containsExactly(d1, d2);

        verify(reservationService).findActiveReservationsForTable(tableId);
        verify(mapper).toResponse(e1);
        verify(mapper).toResponse(e2);
    }

    @Test
    void getHistoryForTable_returnsMappedList() {
        UUID tableId = UUID.randomUUID();

        TableReservationEntity e = TableReservationEntity.builder().id(UUID.randomUUID()).tableId(tableId).build();
        TableReservationResponseDto dto = mock(TableReservationResponseDto.class);

        when(reservationService.findReservationHistoryForTable(tableId)).thenReturn(List.of(e));
        when(mapper.toResponse(e)).thenReturn(dto);

        ResponseEntity<List<TableReservationResponseDto>> resp = controller.getHistoryForTable(tableId);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).containsExactly(dto);

        verify(reservationService).findReservationHistoryForTable(tableId);
        verify(mapper).toResponse(e);
    }

    @Test
    void getReservationsByDate_parsesDate_andReturnsMappedList() {
        String dateStr = "2025-12-12";
        LocalDate expected = LocalDate.parse(dateStr);

        TableReservationEntity e = TableReservationEntity.builder().id(UUID.randomUUID()).build();
        TableReservationResponseDto dto = mock(TableReservationResponseDto.class);

        when(reservationService.findReservationsByDate(expected)).thenReturn(List.of(e));
        when(mapper.toResponse(e)).thenReturn(dto);

        ResponseEntity<List<TableReservationResponseDto>> resp = controller.getReservationsByDate(dateStr);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).containsExactly(dto);

        verify(reservationService).findReservationsByDate(expected);
        verify(mapper).toResponse(e);
    }

    @Test
    void getReservationsByDate_invalidDate_throws() {
        String bad = "not-a-date";
        assertThatThrownBy(() -> controller.getReservationsByDate(bad))
                .isInstanceOf(DateTimeParseException.class);
        verifyNoInteractions(reservationService, mapper);
    }
}