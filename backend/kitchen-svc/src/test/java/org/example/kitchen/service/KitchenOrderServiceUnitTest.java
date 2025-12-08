package org.example.kitchen.service;

import org.example.kitchen.exception.KitchenOrderOperationException;
import org.example.kitchen.model.KitchenOrder;
import org.example.kitchen.model.enums.KitchenOrderStatus;
import org.example.kitchen.repository.KitchenOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Additional unit tests for KitchenOrderService:
 * - not-found scenarios
 * - parameterized transitions checking
 */
@ExtendWith(MockitoExtension.class)
class KitchenOrderServiceUnitTest {

    @Mock
    private KitchenOrderRepository repository;

    private KitchenOrderService kitchenOrderService;

    @BeforeEach
    void setUp() {
        kitchenOrderService = new KitchenOrderService(
                repository,
                1,
                2,
                false,
                "",
                "",
                false
        );
    }

    @Test
    void updateStatus_notFound_throwsKitchenOrderOperationException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kitchenOrderService.updateStatus(id, KitchenOrderStatus.IN_PROGRESS))
                .isInstanceOf(KitchenOrderOperationException.class)
                .hasMessageContaining("Kitchen order not found");
    }

    @Test
    void cancelOrder_notFound_throwsKitchenOrderOperationException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kitchenOrderService.cancelOrder(id))
                .isInstanceOf(KitchenOrderOperationException.class)
                .hasMessageContaining("Kitchen order not found");
    }

    // Parameterized test for many transitions; expected true means valid transition, false means invalid.
    // This exercises the private isValidTransition logic indirectly via updateStatus validation.
    // For invalid transitions we expect KitchenOrderOperationException; for valid ones we stub the repo.
    @ParameterizedTest(name = "{index} from={0} to={1} valid={2}")
    @CsvSource({
            "NEW, PREPARING, true",
            "NEW, CANCELLED, true",
            "PREPARING, IN_PROGRESS, true",
            "PREPARING, READY, true",
            "PREPARING, NEW, false",
            "IN_PROGRESS, READY, true",
            "IN_PROGRESS, PREPARING, false",
            "READY, SERVED, true",
            "READY, PREPARING, false"
    })
    void transition_matrix_checks(KitchenOrderStatus from, KitchenOrderStatus to, boolean valid) {
        UUID id = UUID.randomUUID();
        KitchenOrder existing = new KitchenOrder();
        existing.setId(id);
        existing.setOrderId(UUID.randomUUID());
        existing.setStatus(from);

        when(repository.findById(id)).thenReturn(Optional.of(existing));

        if (valid) {
            // allow save and assert no exception
            when(repository.save(existing)).thenAnswer(inv -> inv.getArgument(0));
            kitchenOrderService.updateStatus(id, to);
        } else {
            // invalid transition should throw KitchenOrderOperationException
            assertThatThrownBy(() -> kitchenOrderService.updateStatus(id, to))
                    .isInstanceOf(KitchenOrderOperationException.class);
        }
    }
}