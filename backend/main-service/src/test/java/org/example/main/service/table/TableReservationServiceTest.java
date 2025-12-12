package org.example.main.service.table;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.table.TableReservationEntity;
import org.example.main.model.enums.ReservationStatus;
import org.example.main.repository.table.TableReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TableReservationService exercising all branches to reach full line coverage.
 */
@ExtendWith(MockitoExtension.class)
class TableReservationServiceTest {

    @Mock
    TableReservationRepository reservationRepository;

    @InjectMocks
    TableReservationService service;

    @Test
    void reserveTable_invalid_whenFromOrToNull_or_toNotAfterFrom() {
        UUID tableId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        
        assertThatThrownBy(() -> service.reserveTable(tableId, null, now.plusHours(1), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid reservation window");

        
        assertThatThrownBy(() -> service.reserveTable(tableId, now, null, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid reservation window");

        
        assertThatThrownBy(() -> service.reserveTable(tableId, now, now, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid reservation window");

        
        assertThatThrownBy(() -> service.reserveTable(tableId, now.plusHours(2), now.plusHours(1), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid reservation window");
    }

    @Test
    void reserveTable_conflict_whenOverlapping() {
        UUID tableId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().plusDays(1);
        OffsetDateTime to = from.plusHours(2);

        TableReservationEntity conflict = TableReservationEntity.builder()
                .id(UUID.randomUUID())
                .tableId(tableId)
                .startTime(from.minusMinutes(10))
                .endTime(to.plusMinutes(10))
                .build();

        when(reservationRepository.findByTableIdAndEndTimeAfterAndStartTimeBefore(tableId, from, to))
                .thenReturn(List.of(conflict));

        assertThatThrownBy(() -> service.reserveTable(tableId, from, to, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Table already reserved for this time");

        verify(reservationRepository).findByTableIdAndEndTimeAfterAndStartTimeBefore(tableId, from, to);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveTable_success_setsFields_and_returnsSaved() {
        UUID tableId = UUID.randomUUID();
        UUID requestedBy = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().plusDays(2);
        OffsetDateTime to = from.plusHours(3);

        when(reservationRepository.findByTableIdAndEndTimeAfterAndStartTimeBefore(tableId, from, to))
                .thenReturn(List.of());

        when(reservationRepository.save(any(TableReservationEntity.class))).thenAnswer(inv -> {
            TableReservationEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        TableReservationEntity saved = service.reserveTable(tableId, from, to, requestedBy, userId);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTableId()).isEqualTo(tableId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getStartTime()).isEqualTo(from);
        assertThat(saved.getEndTime()).isEqualTo(to);
        assertThat(saved.getCreatedBy()).isEqualTo(requestedBy);
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.ACTIVE);

        verify(reservationRepository).findByTableIdAndEndTimeAfterAndStartTimeBefore(tableId, from, to);
        verify(reservationRepository).save(any(TableReservationEntity.class));
    }

    @Test
    void cancelReservation_notFound_throws() {
        UUID rid = UUID.randomUUID();
        when(reservationRepository.findById(rid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelReservation(rid, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reservation not found");

        verify(reservationRepository).findById(rid);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_success_marksCancelled_and_returnsSaved() {
        UUID rid = UUID.randomUUID();
        TableReservationEntity ent = new TableReservationEntity();
        ent.setId(rid);
        ent.setStatus(ReservationStatus.ACTIVE);
        ent.setDeleted(false);
        ent.setUpdatedAt(null);

        when(reservationRepository.findById(rid)).thenReturn(Optional.of(ent));
        when(reservationRepository.save(any(TableReservationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        TableReservationEntity out = service.cancelReservation(rid, UUID.randomUUID());

        assertThat(out.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(out.isDeleted()).isTrue();
        assertThat(out.getUpdatedAt()).isNotNull();

        verify(reservationRepository).findById(rid);
        verify(reservationRepository).save(any(TableReservationEntity.class));
    }

    @Test
    void findActive_and_history_delegation() {
        UUID tableId = UUID.randomUUID();
        TableReservationEntity r = TableReservationEntity.builder().id(UUID.randomUUID()).tableId(tableId).build();
        when(reservationRepository.findByTableIdAndDeletedFalse(tableId)).thenReturn(List.of(r));

        List<TableReservationEntity> active = service.findActiveReservationsForTable(tableId);
        List<TableReservationEntity> history = service.findReservationHistoryForTable(tableId);

        assertThat(active).containsExactly(r);
        assertThat(history).containsExactly(r);

        verify(reservationRepository, times(2)).findByTableIdAndDeletedFalse(tableId);
    }

    @Test
    void findReservationsByDate_nullDate_returnsEmpty() {
        assertThat(service.findReservationsByDate(null)).isEmpty();
    }

    @Test
    void findReservationsByDate_filters_and_sorts_correctly() {
        LocalDate target = LocalDate.of(2025, 12, 12);
        ZoneId zone = ZoneOffset.UTC;
        OffsetDateTime dayStart = target.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime dayEnd = target.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        
        TableReservationEntity r1 = TableReservationEntity.builder()
                .id(UUID.randomUUID())
                .startTime(null)
                .endTime(null)
                .build();

        
        TableReservationEntity r2 = TableReservationEntity.builder()
                .id(UUID.randomUUID())
                .startTime(dayStart.plusHours(2))
                .endTime(null)
                .build();

        
        TableReservationEntity r3 = TableReservationEntity.builder()
                .id(UUID.randomUUID())
                .startTime(dayStart.minusHours(1))
                .endTime(dayStart.plusHours(1))
                .build();

        
        TableReservationEntity r4 = TableReservationEntity.builder()
                .id(UUID.randomUUID())
                .startTime(dayEnd.plusHours(1))
                .endTime(dayEnd.plusHours(2))
                .build();

        
        TableReservationEntity r5 = TableReservationEntity.builder()
                .id(UUID.randomUUID())
                .startTime(dayStart)
                .endTime(null)
                .build();

        
        when(reservationRepository.findAll()).thenReturn(List.of(r1, r4, r2, r5, r3));

        List<TableReservationEntity> res = service.findReservationsByDate(target);

        
        assertThat(res).hasSize(3);
        assertThat(res.get(0)).isEqualTo(r3); 
        assertThat(res.get(1)).isEqualTo(r5);
        assertThat(res.get(2)).isEqualTo(r2);

        verify(reservationRepository).findAll();
    }
}