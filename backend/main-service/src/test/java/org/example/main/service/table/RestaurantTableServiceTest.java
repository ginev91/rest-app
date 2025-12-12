package org.example.main.service.table;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.table.RestaurantTable;
import org.example.main.model.table.TableReservationEntity;
import org.example.main.model.enums.TableStatus;
import org.example.main.repository.table.RestaurantTableRepository;
import org.example.main.repository.table.TableReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.MockedStatic;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RestaurantTableServiceTest {

    @Mock
    RestaurantTableRepository tableRepository;

    @Mock
    TableReservationRepository reservationRepository;

    @InjectMocks
    RestaurantTableService service;

    @BeforeEach
    void setup() {

    }

    @Test
    void findAll_delegates_to_repository() {
        RestaurantTable t = new RestaurantTable();
        when(tableRepository.findAll()).thenReturn(List.of(t));

        List<RestaurantTable> res = service.findAll();
        assertThat(res).containsExactly(t);
        verify(tableRepository).findAll();
    }

    @Test
    void findById_found_and_notFound() {
        UUID id = UUID.randomUUID();
        RestaurantTable t = new RestaurantTable();
        when(tableRepository.findById(id)).thenReturn(Optional.of(t));
        assertThat(service.findById(id)).isSameAs(t);

        UUID missing = UUID.randomUUID();
        when(tableRepository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(missing))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RestaurantTable not found");
    }

    @Test
    void create_null_throwsBadRequest() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Table payload is required");
    }

    @Test
    void create_with_code_parses_tableNumber_and_defaults_and_saves() {
        RestaurantTable in = new RestaurantTable();
        in.setCode(" t7 ");
        in.setSeats(null);
        in.setPinCode(null);

        when(tableRepository.findByTableNumber(7)).thenReturn(Optional.empty());
        when(tableRepository.findByCode("T7")).thenReturn(Optional.empty());
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> {
            RestaurantTable r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        RestaurantTable out = service.create(in);
        assertThat(out.getCode()).isEqualTo("T7");
        assertThat(out.getTableNumber()).isEqualTo(7);
        assertThat(out.getSeats()).isEqualTo(4);
        assertThat(out.getPinCode()).isNotNull();
        assertThat(out.getStatus()).isEqualTo(TableStatus.AVAILABLE);

        verify(tableRepository).save(any(RestaurantTable.class));
    }

    @Test
    void create_conflict_on_tableNumber() {
        RestaurantTable in = new RestaurantTable();
        in.setTableNumber(5);
        when(tableRepository.findByTableNumber(5)).thenReturn(Optional.of(new RestaurantTable()));

        assertThatThrownBy(() -> service.create(in))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Table number already exists");
    }

    @Test
    void create_conflict_on_code() {
        RestaurantTable in = new RestaurantTable();
        in.setCode("X1");
        when(tableRepository.findByCode("X1")).thenReturn(Optional.of(new RestaurantTable()));

        assertThatThrownBy(() -> service.create(in))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Table code already exists");
    }

    @Test
    void create_invalid_pin_throws_badRequest() {
        RestaurantTable in = new RestaurantTable();
        in.setPinCode("abcd");

        
        try (MockedStatic<RestaurantTableUtils> utils = mockStatic(RestaurantTableUtils.class)) {
            utils.when(() -> RestaurantTableUtils.sanitizePin("abcd")).thenReturn("abcd");
            utils.when(() -> RestaurantTableUtils.isValidPin("abcd")).thenReturn(false);
            
            utils.when(RestaurantTableUtils::generatePinCode).thenReturn("0000");

            assertThatThrownBy(() -> service.create(in))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("pinCode must be 1..4 digits");
        }
    }

    @Test
    void create_save_throws_dataIntegrity_converted_to_conflict() {
        RestaurantTable in = new RestaurantTable();
        in.setTableNumber(99);
        when(tableRepository.findByTableNumber(99)).thenReturn(Optional.empty());
        when(tableRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(tableRepository.save(any(RestaurantTable.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.create(in))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot create table due to constraint violation");
    }

    @Test
    void update_changes_code_and_tableNumber_and_seats_and_pin_generation() {
        UUID id = UUID.randomUUID();
        RestaurantTable existing = new RestaurantTable();
        existing.setId(id);
        existing.setCode("OLD");
        existing.setTableNumber(1);
        existing.setSeats(2);
        existing.setPinCode(null);

        when(tableRepository.findById(id)).thenReturn(Optional.of(existing));
        when(tableRepository.findByCode("T10")).thenReturn(Optional.empty());
        when(tableRepository.findByTableNumber(10)).thenReturn(Optional.empty());
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        RestaurantTable changes = new RestaurantTable();
        changes.setCode(" t10 ");
        changes.setSeats(6);
        changes.setPinCode("0099");

        RestaurantTable res = service.update(id, changes);
        assertThat(res.getCode()).isEqualTo("T10");
        assertThat(res.getTableNumber()).isEqualTo(10);
        assertThat(res.getSeats()).isEqualTo(6);
        assertThat(res.getPinCode()).isEqualTo("0099");
    }

    @Test
    void update_conflict_when_newCode_taken_by_other() {
        UUID id = UUID.randomUUID();
        RestaurantTable existing = new RestaurantTable();
        existing.setId(id);
        when(tableRepository.findById(id)).thenReturn(Optional.of(existing));

        RestaurantTable other = new RestaurantTable();
        other.setId(UUID.randomUUID());
        when(tableRepository.findByCode("C1")).thenReturn(Optional.of(other));

        RestaurantTable changes = new RestaurantTable();
        changes.setCode("c1");

        assertThatThrownBy(() -> service.update(id, changes))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Table code already exists");
    }

    @Test
    void update_conflict_when_newTableNumber_taken_by_other() {
        UUID id = UUID.randomUUID();
        RestaurantTable existing = new RestaurantTable();
        existing.setId(id);
        existing.setTableNumber(2);
        when(tableRepository.findById(id)).thenReturn(Optional.of(existing));

        RestaurantTable other = new RestaurantTable();
        other.setId(UUID.randomUUID());
        when(tableRepository.findByTableNumber(99)).thenReturn(Optional.of(other));

        RestaurantTable changes = new RestaurantTable();
        changes.setTableNumber(99);

        assertThatThrownBy(() -> service.update(id, changes))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Table number already exists");
    }

    @Test
    void update_pin_invalid_throws_badRequest_when_pin_non_numeric_or_too_long() {
        UUID id = UUID.randomUUID();
        RestaurantTable existing = new RestaurantTable();
        existing.setId(id);
        existing.setPinCode(null);
        when(tableRepository.findById(id)).thenReturn(Optional.of(existing));

        RestaurantTable changes = new RestaurantTable();
        changes.setPinCode("12345");

        
        try (MockedStatic<RestaurantTableUtils> utils = mockStatic(RestaurantTableUtils.class)) {
            utils.when(() -> RestaurantTableUtils.sanitizePin("12345")).thenReturn("12345");
            utils.when(() -> RestaurantTableUtils.isValidPin("12345")).thenReturn(false);
            utils.when(RestaurantTableUtils::generatePinCode).thenReturn("0000");

            assertThatThrownBy(() -> service.update(id, changes))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("pinCode must be 1..4 digits");
        }
    }

    @Test
    void update_save_throws_converted_to_conflict() {
        UUID id = UUID.randomUUID();
        RestaurantTable existing = new RestaurantTable();
        existing.setId(id);
        when(tableRepository.findById(id)).thenReturn(Optional.of(existing));
        when(tableRepository.save(any(RestaurantTable.class))).thenThrow(new DataIntegrityViolationException("x"));

        RestaurantTable changes = new RestaurantTable();
        changes.setSeats(5);

        assertThatThrownBy(() -> service.update(id, changes))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot update table due to constraint violation");
    }

    @Test
    void delete_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(tableRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_occupied_conflict_when_occupiedUntil_in_future() {
        UUID id = UUID.randomUUID();
        RestaurantTable t = new RestaurantTable();
        t.setId(id);
        t.setStatus(TableStatus.OCCUPIED);
        t.setOccupiedUntil(OffsetDateTime.now().plusMinutes(10));
        when(tableRepository.findById(id)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot delete table while it is occupied");
    }

    @Test
    void delete_futureReservations_conflict() {
        UUID id = UUID.randomUUID();
        RestaurantTable t = new RestaurantTable();
        t.setId(id);
        t.setStatus(TableStatus.AVAILABLE);
        when(tableRepository.findById(id)).thenReturn(Optional.of(t));

        TableReservationEntity r = TableReservationEntity.builder()
                .id(UUID.randomUUID())
                .tableId(id)
                .endTime(OffsetDateTime.now().plusDays(1))
                .build();

        when(reservationRepository.findByTableId(id)).thenReturn(List.of(r));

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot delete table with future reservations");
    }

    @Test
    void delete_db_constraint_failure_converted_to_conflict() {
        UUID id = UUID.randomUUID();
        RestaurantTable t = new RestaurantTable();
        t.setId(id);
        t.setStatus(TableStatus.AVAILABLE);
        when(tableRepository.findById(id)).thenReturn(Optional.of(t));
        when(reservationRepository.findByTableId(id)).thenReturn(List.of());

        doThrow(new DataIntegrityViolationException("fk")).when(tableRepository).deleteById(id);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot delete table with existing relations");
    }

    @Test
    void occupyTable_null_noop() {
        service.occupyTable(null, 10);
        verifyNoInteractions(tableRepository);
    }

    @Test
    void occupyTable_updates_byTableNumber() {
        Integer tn = 3;
        RestaurantTable existing = new RestaurantTable();
        existing.setTableNumber(tn);
        when(tableRepository.findByTableNumber(tn)).thenReturn(Optional.of(existing));
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        service.occupyTable(tn, 5);

        verify(tableRepository).save(any(RestaurantTable.class));
        assertThat(existing.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(existing.getOccupiedUntil()).isNotNull();
    }

    @Test
    void occupyTable_byCode_setsTableNumberIfMissing() {
        Integer tn = 7;
        String code = "T7";
        RestaurantTable existing = new RestaurantTable();
        existing.setCode(code);
        existing.setTableNumber(null);
        when(tableRepository.findByTableNumber(tn)).thenReturn(Optional.empty());
        when(tableRepository.findByCode(code)).thenReturn(Optional.of(existing));
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        service.occupyTable(tn, 10);

        assertThat(existing.getTableNumber()).isEqualTo(tn);
        assertThat(existing.getStatus()).isEqualTo(TableStatus.OCCUPIED);
    }

    @Test
    void occupyTable_createsNewTable_whenNotFound() {
        Integer tn = 99;
        String code = "T99";
        when(tableRepository.findByTableNumber(tn)).thenReturn(Optional.empty());
        when(tableRepository.findByCode(code)).thenReturn(Optional.empty());
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> {
            RestaurantTable r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        service.occupyTable(tn, 20);

        ArgumentCaptor<RestaurantTable> cap = ArgumentCaptor.forClass(RestaurantTable.class);
        verify(tableRepository).save(cap.capture());
        RestaurantTable saved = cap.getValue();
        assertThat(saved.getCode()).isEqualTo(code);
        assertThat(saved.getTableNumber()).isEqualTo(tn);
        assertThat(saved.getStatus()).isEqualTo(TableStatus.OCCUPIED);
    }

    @Test
    void occupyTable_handlesConcurrentInsert_and_updates_existing() {
        Integer tn = 8;
        String code = "T8";

        when(tableRepository.findByTableNumber(tn)).thenReturn(Optional.empty());

        when(tableRepository.save(any(RestaurantTable.class)))
                .thenThrow(new DataIntegrityViolationException("concurrent"))
                .thenAnswer(inv -> inv.getArgument(0));

        RestaurantTable existing = new RestaurantTable();
        existing.setId(UUID.randomUUID());
        existing.setCode(code);
        when(tableRepository.findByCode(code)).thenReturn(Optional.of(existing));


        service.occupyTable(tn, 15);

        verify(tableRepository, atLeast(1)).findByCode(code);
        verify(tableRepository, atLeast(1)).save(any(RestaurantTable.class));
    }

    @Test
    void occupyTable_rethrows_whenConcurrentInsert_and_no_existing() {
        Integer tn = 123;
        String code = "T123";
        when(tableRepository.findByTableNumber(tn)).thenReturn(Optional.empty());
        when(tableRepository.findByCode(code)).thenReturn(Optional.empty());
        when(tableRepository.save(any(RestaurantTable.class))).thenThrow(new DataIntegrityViolationException("uniq"));

        assertThatThrownBy(() -> service.occupyTable(tn, 5))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reserveTable_invalid_window_throws() {
        UUID tid = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().plusHours(2);
        OffsetDateTime to = from.minusHours(1);
        assertThatThrownBy(() -> service.reserveTable(tid, from, to, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid reservation window");
    }

    @Test
    void reserveTable_conflict_when_conflicting_reservation() {
        UUID tid = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().plusHours(1);
        OffsetDateTime to = from.plusHours(2);

        TableReservationEntity conflict = TableReservationEntity.builder()
                .id(UUID.randomUUID())
                .tableId(tid)
                .startTime(from.minusMinutes(10))
                .endTime(to.plusMinutes(10))
                .build();

        when(reservationRepository.findByTableIdAndEndTimeAfterAndStartTimeBefore(tid, from, to))
                .thenReturn(List.of(conflict));

        assertThatThrownBy(() -> service.reserveTable(tid, from, to, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Table already reserved for this time");
    }

    @Test
    void reserveTable_success_saves_and_returns_entity() {
        UUID tid = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().plusDays(1);
        OffsetDateTime to = from.plusHours(2);

        when(reservationRepository.findByTableIdAndEndTimeAfterAndStartTimeBefore(tid, from, to))
                .thenReturn(List.of());
        when(reservationRepository.save(any(TableReservationEntity.class))).thenAnswer(inv -> {
            TableReservationEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        TableReservationEntity out = service.reserveTable(tid, from, to, UUID.randomUUID(), UUID.randomUUID());
        assertThat(out.getId()).isNotNull();
        verify(reservationRepository).save(any(TableReservationEntity.class));
    }

    @Test
    void findByTableNumber_and_findReservationsForTable_delegation() {
        Integer tn = 4;
        UUID tableId = UUID.randomUUID();
        RestaurantTable rt = new RestaurantTable();
        rt.setTableNumber(tn);
        when(tableRepository.findByTableNumber(tn)).thenReturn(Optional.of(rt));

        Optional<RestaurantTable> maybe = service.findByTableNumber(tn);
        assertThat(maybe).isPresent().contains(rt);

        TableReservationEntity r = TableReservationEntity.builder().id(UUID.randomUUID()).tableId(tableId).build();
        when(reservationRepository.findByTableId(tableId)).thenReturn(List.of(r));

        List<TableReservationEntity> list = service.findReservationsForTable(tableId);
        assertThat(list).containsExactly(r);
    }
}