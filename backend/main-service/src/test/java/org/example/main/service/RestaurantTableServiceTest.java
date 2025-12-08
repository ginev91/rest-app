package org.example.main.service;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.RestaurantTable;
import org.example.main.model.TableReservationEntity;
import org.example.main.model.enums.TableStatus;
import org.example.main.repository.RestaurantTableRepository;
import org.example.main.repository.TableReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantTableServiceTest {

    @Mock
    RestaurantTableRepository tableRepository;

    @Mock
    TableReservationRepository reservationRepository;

    @InjectMocks
    RestaurantTableService service;

    @BeforeEach
    void setUp() {
        // no-op
    }

    @Test
    void occupyTable_noop_whenTableNumberNull() {
        service.occupyTable(null, 10);
        verifyNoInteractions(tableRepository);
    }

    @Test
    void occupyTable_updatesExistingByTableNumber() {
        Integer tableNumber = 5;
        RestaurantTable existing = new RestaurantTable();
        existing.setId(UUID.randomUUID());
        existing.setTableNumber(tableNumber);
        existing.setStatus(TableStatus.AVAILABLE);

        when(tableRepository.findByTableNumber(tableNumber)).thenReturn(Optional.of(existing));
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        service.occupyTable(tableNumber, 15);

        ArgumentCaptor<RestaurantTable> captor = ArgumentCaptor.forClass(RestaurantTable.class);
        verify(tableRepository).save(captor.capture());
        RestaurantTable saved = captor.getValue();

        assertThat(saved.getTableNumber()).isEqualTo(tableNumber);
        assertThat(saved.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(saved.getOccupiedUntil()).isNotNull();
    }

    @Test
    void occupyTable_updatesExistingByCode_and_setsTableNumberIfMissing() {
        Integer tableNumber = 7;
        String code = "T" + tableNumber;
        RestaurantTable existing = new RestaurantTable();
        existing.setId(UUID.randomUUID());
        existing.setCode(code);
        existing.setTableNumber(null); // simulates older row
        existing.setStatus(TableStatus.AVAILABLE);

        when(tableRepository.findByTableNumber(tableNumber)).thenReturn(Optional.empty());
        when(tableRepository.findByCode(code)).thenReturn(Optional.of(existing));
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        service.occupyTable(tableNumber, 20);

        ArgumentCaptor<RestaurantTable> captor = ArgumentCaptor.forClass(RestaurantTable.class);
        verify(tableRepository).save(captor.capture());
        RestaurantTable saved = captor.getValue();

        assertThat(saved.getCode()).isEqualTo(code);
        assertThat(saved.getTableNumber()).isEqualTo(tableNumber);
        assertThat(saved.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(saved.getOccupiedUntil()).isNotNull();
    }

    @Test
    void occupyTable_createsNewTable_whenNotFound() {
        Integer tableNumber = 42;
        String code = "T" + tableNumber;

        when(tableRepository.findByTableNumber(tableNumber)).thenReturn(Optional.empty());
        when(tableRepository.findByCode(code)).thenReturn(Optional.empty());
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> {
            RestaurantTable t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        service.occupyTable(tableNumber, 30);

        ArgumentCaptor<RestaurantTable> captor = ArgumentCaptor.forClass(RestaurantTable.class);
        verify(tableRepository).save(captor.capture());
        RestaurantTable saved = captor.getValue();

        assertThat(saved.getCode()).isEqualTo(code);
        assertThat(saved.getSeats()).isEqualTo(4);
        assertThat(saved.getPinCode()).hasSize(6);
        assertThat(saved.getTableNumber()).isEqualTo(tableNumber);
        assertThat(saved.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(saved.getOccupiedUntil()).isNotNull();
    }

    @Test
    void occupyTable_handlesConcurrentInsert_byReloadingAndUpdating() {
        Integer tableNumber = 11;
        String code = "T" + tableNumber;

        RestaurantTable newTable = new RestaurantTable();
        newTable.setCode(code);
        newTable.setTableNumber(tableNumber);
        newTable.setSeats(4);
        newTable.setPinCode("ABC123");
        newTable.setStatus(TableStatus.AVAILABLE);

        RestaurantTable existing = new RestaurantTable();
        existing.setId(UUID.randomUUID());
        existing.setCode(code);
        existing.setTableNumber(null); // existing without tableNumber

        // first save throws DataIntegrityViolationException (simulate concurrent insert)
        when(tableRepository.findByTableNumber(tableNumber)).thenReturn(Optional.empty());
        when(tableRepository.findByCode(code)).thenReturn(Optional.empty(), Optional.of(existing)); // second time return existing
        when(tableRepository.save(any(RestaurantTable.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"))
                .thenAnswer(inv -> inv.getArgument(0));

        // Call - should not throw; should reload and save existing
        service.occupyTable(tableNumber, 25);

        // verify save was attempted at least twice (first failed, second for existing)
        verify(tableRepository, atLeast(2)).save(any(RestaurantTable.class));
        verify(tableRepository, atLeastOnce()).findByCode(code);
    }

    @Test
    void occupyTable_rethrowsWhenConcurrentInsertAndNoExisting() {
        Integer tableNumber = 99;
        String code = "T" + tableNumber;

        when(tableRepository.findByTableNumber(tableNumber)).thenReturn(Optional.empty());
        when(tableRepository.findByCode(code)).thenReturn(Optional.empty());
        when(tableRepository.save(any(RestaurantTable.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> service.occupyTable(tableNumber, 10))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(tableRepository).save(any(RestaurantTable.class));
        verify(tableRepository, atLeastOnce()).findByCode(code);
    }

    @Test
    void reserveTable_validatesWindow_andThrowsBadRequest() {
        UUID tableId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to = from.minusMinutes(5); // invalid: to is before from

        assertThatThrownBy(() -> service.reserveTable(tableId, from, to, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void reserveTable_conflict_whenOverlappingReservations() {
        UUID tableId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().plusHours(1);
        OffsetDateTime to = from.plusHours(2);

        TableReservationEntity conflict = TableReservationEntity.builder()
                .id(UUID.randomUUID())
                .tableId(tableId)
                .startTime(from.minusMinutes(10))
                .endTime(to.plusMinutes(10))
                .build();

        when(reservationRepository.findByTableIdAndEndTimeAfterAndStartTimeBefore(eq(tableId), any(), any()))
                .thenReturn(List.of(conflict));

        assertThatThrownBy(() -> service.reserveTable(tableId, from, to, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void reserveTable_success_createsReservation() {
        UUID tableId = UUID.randomUUID();
        UUID requestedBy = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().plusDays(1);
        OffsetDateTime to = from.plusHours(2);

        when(reservationRepository.findByTableIdAndEndTimeAfterAndStartTimeBefore(eq(tableId), eq(from), eq(to)))
                .thenReturn(List.of());

        when(reservationRepository.save(any(TableReservationEntity.class))).thenAnswer(inv -> {
            TableReservationEntity r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        TableReservationEntity saved = service.reserveTable(tableId, from, to, requestedBy, userId);

        ArgumentCaptor<TableReservationEntity> captor = ArgumentCaptor.forClass(TableReservationEntity.class);
        verify(reservationRepository).save(captor.capture());
        TableReservationEntity arg = captor.getValue();

        assertThat(arg.getTableId()).isEqualTo(tableId);
        assertThat(arg.getUserId()).isEqualTo(userId);
        assertThat(arg.getStartTime()).isEqualTo(from);
        assertThat(arg.getEndTime()).isEqualTo(to);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findAll_and_findById_and_crud_behaviour() {
        // findAll delegation
        RestaurantTable one = new RestaurantTable();
        one.setId(UUID.randomUUID());
        when(tableRepository.findAll()).thenReturn(List.of(one));
        assertThat(service.findAll()).containsExactly(one);

        // findById existing
        when(tableRepository.findById(one.getId())).thenReturn(Optional.of(one));
        assertThat(service.findById(one.getId())).isEqualTo(one);

        // findById missing -> ResourceNotFoundException
        UUID missing = UUID.randomUUID();
        when(tableRepository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(missing)).isInstanceOf(ResourceNotFoundException.class);

        // create & update & delete flows (basic)
        RestaurantTable toCreate = new RestaurantTable();
        toCreate.setTableNumber(10);
        when(tableRepository.save(toCreate)).thenReturn(toCreate);
        assertThat(service.create(toCreate)).isEqualTo(toCreate);

        // update: change seats and code
        RestaurantTable existing2 = new RestaurantTable();
        existing2.setId(UUID.randomUUID());
        existing2.setSeats(2);
        when(tableRepository.findById(existing2.getId())).thenReturn(Optional.of(existing2));
        RestaurantTable changes = new RestaurantTable();
        changes.setSeats(6);
        changes.setCode("C1");
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));
        RestaurantTable updated = service.update(existing2.getId(), changes);
        assertThat(updated.getSeats()).isEqualTo(6);
        assertThat(updated.getCode()).isEqualTo("C1");

        // delete missing -> ResourceNotFoundException
        UUID del = UUID.randomUUID();
        when(tableRepository.existsById(del)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(del)).isInstanceOf(ResourceNotFoundException.class);

        // delete existing -> call repository.deleteById
        when(tableRepository.existsById(existing2.getId())).thenReturn(true);
        doNothing().when(tableRepository).deleteById(existing2.getId());
        service.delete(existing2.getId());
        verify(tableRepository).deleteById(existing2.getId());
    }

    @Test
    void findByTableNumber_and_findReservationsForTable_delegation() {
        Integer num = 3;
        UUID tableId = UUID.randomUUID();
        RestaurantTable rt = new RestaurantTable();
        rt.setTableNumber(num);

        when(tableRepository.findByTableNumber(num)).thenReturn(Optional.of(rt));
        assertThat(service.findByTableNumber(num)).isPresent();

        TableReservationEntity r = TableReservationEntity.builder().id(UUID.randomUUID()).tableId(tableId).build();
        when(reservationRepository.findByTableId(tableId)).thenReturn(List.of(r));
        assertThat(service.findReservationsForTable(tableId)).containsExactly(r);
    }
}