package org.example.main.integration;

import org.example.main.service.TableReservationService;
import org.example.main.repository.TableReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration test for the service layer using your Postgres test DB.
 */
@SpringBootTest(properties = {
        "jwt.secret=0123456789abcdefghijklmnopqrstuv"
})
@ActiveProfiles("test")
class TableReservationServiceIntegrationTest {

    @Autowired
    private TableReservationService reservationService;

    @Autowired
    private TableReservationRepository reservationRepository;

    @Test
    @Transactional
    void reserve_conflict_and_cancel_flow_withRepository() {
        UUID tableId = UUID.randomUUID();
        UUID requestedBy = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now().plusHours(1);
        OffsetDateTime to = from.plusHours(2);

        var created = reservationService.reserveTable(tableId, from, to, requestedBy, userId);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(org.example.main.model.enums.ReservationStatus.ACTIVE);

        OffsetDateTime overlapFrom = from.plusMinutes(30);
        OffsetDateTime overlapTo = to.plusMinutes(30);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                reservationService.reserveTable(tableId, overlapFrom, overlapTo, UUID.randomUUID(), UUID.randomUUID())
        );

        var cancelled = reservationService.cancelReservation(created.getId(), UUID.randomUUID());
        assertThat(cancelled.getStatus()).isEqualTo(org.example.main.model.enums.ReservationStatus.CANCELLED);
        assertThat(cancelled.isDeleted()).isTrue();

        var persisted = reservationRepository.findById(created.getId()).orElse(null);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getStatus()).isEqualTo(org.example.main.model.enums.ReservationStatus.CANCELLED);
        assertThat(persisted.isDeleted()).isTrue();

        reservationRepository.deleteById(created.getId());
    }
}