package org.example.main.controller.table;

import org.example.main.dto.request.table.OccupyRequest;
import org.example.main.dto.request.table.ReservationRequestDto;
import org.example.main.model.table.RestaurantTable;
import org.example.main.model.table.TableReservationEntity;
import org.example.main.service.table.RestaurantTableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantTableControllerTest {

    @Mock
    private RestaurantTableService service;

    @Test
    void getTables_delegatesToService_and_returnsList() {
        RestaurantTable t1 = new RestaurantTable();
        t1.setCode("T1");
        RestaurantTable t2 = new RestaurantTable();
        t2.setCode("T2");

        when(service.findAll()).thenReturn(List.of(t1, t2));

        RestaurantTableController ctrl = new RestaurantTableController(service);
        ResponseEntity<List<RestaurantTable>> resp = ctrl.getTables();

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).containsExactly(t1, t2);
        verify(service).findAll();
    }

    @Test
    void reserve_callsService_withExpectedArguments_and_returnsSavedReservation() {
        UUID tableId = UUID.randomUUID();
        UUID requester = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().plusDays(1);
        OffsetDateTime to = from.plusHours(2);

        ReservationRequestDto req = new ReservationRequestDto();
        req.setFrom(from);
        req.setTo(to);
        req.setUserId(userId);
        req.setRequestedBy(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        TableReservationEntity saved = new TableReservationEntity();
        saved.setTableId(tableId);
        saved.setUserId(userId);

        when(service.reserveTable(eq(tableId), eq(from), eq(to), eq(requester), eq(userId))).thenReturn(saved);

        RestaurantTableController ctrl = new RestaurantTableController(service);
        ResponseEntity<TableReservationEntity> resp = ctrl.reserve(tableId, req, requester);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isSameAs(saved);

        verify(service).reserveTable(tableId, from, to, requester, userId);
    }

    @Test
    void getReservations_delegatesToService_and_returnsList() {
        UUID tableId = UUID.randomUUID();
        TableReservationEntity r1 = new TableReservationEntity();
        TableReservationEntity r2 = new TableReservationEntity();

        when(service.findReservationsForTable(tableId)).thenReturn(List.of(r1, r2));

        RestaurantTableController ctrl = new RestaurantTableController(service);
        ResponseEntity<List<TableReservationEntity>> resp = ctrl.getReservations(tableId);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).containsExactly(r1, r2);
        verify(service).findReservationsForTable(tableId);
    }

    @Test
    void occupy_callsService_withRequestValues() {
        OccupyRequest req = new OccupyRequest();
        req.tableNumber = 5;
        req.minutes = 30;

        RestaurantTableController ctrl = new RestaurantTableController(service);
        ResponseEntity<Void> resp = ctrl.occupy(req);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        verify(service).occupyTable(5, 30);
    }
}