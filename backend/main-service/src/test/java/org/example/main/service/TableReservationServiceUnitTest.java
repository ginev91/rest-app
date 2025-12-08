package org.example.main.service;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.TableReservationEntity;
import org.example.main.model.enums.ReservationStatus;
import org.example.main.repository.TableReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableReservationServiceUnitTest {

    @Mock
    private TableReservationRepository reservationRepository;

    @InjectMocks
    private TableReservationService service;

    @BeforeEach
    void setUp() {
        
    }

    @Test
    void reserveTable_throwsBadRequest_forInvalidWindow() {
        UUID tableId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to = from.minusMinutes(5); 

        assertThatThrownBy(() -> service.reserveTable(tableId, from, to, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid reservation window");

        assertThatThrownBy(() -> service.reserveTable(tableId, null, to, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.reserveTable(tableId, from, null, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void reserveTable_throwsConflict_whenOverlappingReservationsExist() {
        UUID tableId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now().plusHours(1);
        OffsetDateTime to = from.plusHours(2);

        TableReservationEntity conflict = new TableReservationEntity();
        conflict.setId(UUID.randomUUID());
        conflict.setTableId(tableId);
        conflict.setStartTime(from.minusMinutes(10));
        conflict.setEndTime(to.plusMinutes(10));

        when(reservationRepository.findByTableIdAndEndTimeAfterAndStartTimeBefore(eq(tableId), eq(from), eq(to)))
                .thenReturn(List.of(conflict));

        assertThatThrownBy(() -> service.reserveTable(tableId, from, to, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Table already reserved");

        verify(reservationRepository).findByTableIdAndEndTimeAfterAndStartTimeBefore(eq(tableId), eq(from), eq(to));
        verifyNoMoreInteractions(reservationRepository);
    }

    @Test
    void reserveTable_success_savesAndReturnsEntity() {
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

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTableId()).isEqualTo(tableId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getStartTime()).isEqualTo(from);
        assertThat(saved.getEndTime()).isEqualTo(to);
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.ACTIVE);

        verify(reservationRepository).findByTableIdAndEndTimeAfterAndStartTimeBefore(eq(tableId), eq(from), eq(to));
        verify(reservationRepository).save(any(TableReservationEntity.class));
    }

    @Test
    void cancelReservation_throwsWhenMissing() {
        UUID id = UUID.randomUUID();

        when(reservationRepository.findById(eq(id))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelReservation(id, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(reservationRepository).findById(eq(id));
        verifyNoMoreInteractions(reservationRepository);
    }

    @Test
    void cancelReservation_updatesStatusAndDeletes() {
        UUID id = UUID.randomUUID();

        TableReservationEntity existing = new TableReservationEntity();
        existing.setId(id);
        existing.setStatus(ReservationStatus.ACTIVE);
        existing.setDeleted(false);

        when(reservationRepository.findById(eq(id))).thenReturn(Optional.of(existing));
        when(reservationRepository.save(any(TableReservationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        TableReservationEntity result = service.cancelReservation(id, UUID.randomUUID());

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(result.isDeleted()).isTrue();
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(reservationRepository).findById(eq(id));
        verify(reservationRepository).save(any(TableReservationEntity.class));
    }
}