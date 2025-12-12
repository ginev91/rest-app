package org.example.main.controller.table;

import org.example.main.dto.request.table.OccupyRequest;
import org.example.main.dto.request.table.ReservationRequestDto;
import org.example.main.model.table.RestaurantTable;
import org.example.main.model.table.TableReservationEntity;
import org.example.main.model.enums.TableStatus;
import org.example.main.service.table.RestaurantTableService;
import org.example.main.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantTableControllerTest {

    @Mock
    RestaurantTableService service;

    @InjectMocks
    RestaurantTableController controller;

    @Test
    void getTables_returnsListFromService() {
        RestaurantTable t1 = new RestaurantTable();
        t1.setId(UUID.randomUUID());
        RestaurantTable t2 = new RestaurantTable();
        t2.setId(UUID.randomUUID());

        when(service.findAll()).thenReturn(List.of(t1, t2));

        ResponseEntity<List<RestaurantTable>> resp = controller.getTables();

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).containsExactly(t1, t2);
        verify(service).findAll();
    }

    @Test
    void getTable_returnsSingleTable() {
        UUID id = UUID.randomUUID();
        RestaurantTable t = new RestaurantTable();
        t.setId(id);
        when(service.findById(id)).thenReturn(t);

        ResponseEntity<RestaurantTable> resp = controller.getTable(id);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(t);
        verify(service).findById(id);
    }

    @Test
    void getTable_propagatesNotFound() {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenThrow(new ResourceNotFoundException("not found"));

        assertThatThrownBy(() -> controller.getTable(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
        verify(service).findById(id);
    }

    @Test
    void createTable_returnsCreatedWithLocation() {
        RestaurantTable in = new RestaurantTable();
        RestaurantTable saved = new RestaurantTable();
        UUID id = UUID.randomUUID();
        saved.setId(id);

        when(service.create(in)).thenReturn(saved);

        ResponseEntity<RestaurantTable> resp = controller.createTable(in);

        assertThat(resp.getStatusCodeValue()).isEqualTo(201);
        assertThat(resp.getHeaders().getLocation()).isEqualTo(URI.create("/api/tables/" + id));
        assertThat(resp.getBody()).isSameAs(saved);
        verify(service).create(in);
    }

    @Test
    void updateTable_returnsUpdated() {
        UUID id = UUID.randomUUID();
        RestaurantTable changes = new RestaurantTable();
        RestaurantTable updated = new RestaurantTable();
        updated.setId(id);

        when(service.update(id, changes)).thenReturn(updated);

        ResponseEntity<RestaurantTable> resp = controller.updateTable(id, changes);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(updated);
        verify(service).update(id, changes);
    }

    @Test
    void deleteTable_noContent_and_propagatesNotFound() {
        UUID id = UUID.randomUUID();
        
        doNothing().when(service).delete(id);
        ResponseEntity<Void> resp = controller.deleteTable(id);
        assertThat(resp.getStatusCodeValue()).isEqualTo(204);
        verify(service).delete(id);

        
        doThrow(new ResourceNotFoundException("nope")).when(service).delete(id);
        assertThatThrownBy(() -> controller.deleteTable(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nope");
        verify(service, times(2)).delete(id); 
    }

    @Test
    void reserve_callsService_and_returnsReservation_whenRequesterHeaderPresent() {
        UUID tableId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().plusHours(1);
        OffsetDateTime to = from.plusHours(2);
        UUID userId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        ReservationRequestDto req = new ReservationRequestDto();
        req.setFrom(from);
        req.setTo(to);
        req.setUserId(userId);

        TableReservationEntity saved = TableReservationEntity.builder()
                .id(UUID.randomUUID())
                .tableId(tableId)
                .startTime(from)
                .endTime(to)
                .userId(userId)
                .createdBy(requesterId)
                .build();

        when(service.reserveTable(tableId, from, to, requesterId, userId)).thenReturn(saved);

        ResponseEntity<TableReservationEntity> resp = controller.reserve(tableId, req, requesterId);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(saved);
        verify(service).reserveTable(tableId, from, to, requesterId, userId);
    }

    @Test
    void reserve_callsService_and_returnsReservation_whenRequesterHeaderMissing() {
        UUID tableId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().plusHours(1);
        OffsetDateTime to = from.plusHours(2);
        UUID userId = UUID.randomUUID();

        ReservationRequestDto req = new ReservationRequestDto();
        req.setFrom(from);
        req.setTo(to);
        req.setUserId(userId);

        TableReservationEntity saved = TableReservationEntity.builder()
                .id(UUID.randomUUID())
                .tableId(tableId)
                .startTime(from)
                .endTime(to)
                .userId(userId)
                .createdBy(null)
                .build();

        when(service.reserveTable(tableId, from, to, null, userId)).thenReturn(saved);

        ResponseEntity<TableReservationEntity> resp = controller.reserve(tableId, req, null);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(saved);
        verify(service).reserveTable(tableId, from, to, null, userId);
    }

    @Test
    void reserve_propagatesServiceValidationError() {
        UUID tableId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().plusHours(2);
        OffsetDateTime to = from.minusHours(1);

        ReservationRequestDto req = new ReservationRequestDto();
        req.setFrom(from);
        req.setTo(to);
        req.setUserId(UUID.randomUUID());

        when(service.reserveTable(eq(tableId), any(), any(), any(), any()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "bad window"));

        assertThatThrownBy(() -> controller.reserve(tableId, req, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("bad window");
        verify(service).reserveTable(eq(tableId), any(), any(), any(), any());
    }

    @Test
    void getReservations_returnsList() {
        UUID tableId = UUID.randomUUID();
        TableReservationEntity r = TableReservationEntity.builder()
                .id(UUID.randomUUID())
                .tableId(tableId)
                .build();

        when(service.findReservationsForTable(tableId)).thenReturn(List.of(r));

        ResponseEntity<List<TableReservationEntity>> resp = controller.getReservations(tableId);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).containsExactly(r);
        verify(service).findReservationsForTable(tableId);
    }

    @Test
    void occupy_callsService_and_returnsOk() {
        OccupyRequest req = new OccupyRequest();
        
        req.tableNumber = 42;
        req.minutes = 15;

        
        doNothing().when(service).occupyTable(42, 15);

        ResponseEntity<Void> resp = controller.occupy(req);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        verify(service).occupyTable(42, 15);
    }
}